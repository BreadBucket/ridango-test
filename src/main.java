import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


class Main {
	static final String stopTimes_Path = "test_gtfs/stop_times.txt";
	static final String trips_path = "test_gtfs/trips.txt";
	static final String routes_path = "test_gtfs/routes.txt";
	static final String stops_path = "test_gtfs/stops.txt";
	
	static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
	static final Duration maxLookahead = Duration.ofHours(2);
	
	
	static LocalDateTime getNow(){
		LocalDateTime t = LocalDateTime.now();
		// t = t.withHour(12).withMinute(0).withSecond(0).withNano(0);	// DEBUG at 12:00
		return t;
	}
	
	
	public static void main(String[] args) throws Exception {
		// args = new String[]{"10", "1"};	// DEBUG
		String id = null;
		int num = -1;
		boolean relative = true;
		
		if (args.length >= 1){
			id = args[0];
		} else {
			System.err.println("error: Missing stop id argument.");
			System.exit(1);
		}
		
		if (args.length >= 2){
			
			try {
				num = Integer.parseInt(args[1]);
			} catch (Exception e){}
			
			if (num <= 0){
				System.err.println("error: Bus count must be a positive integer.");
				System.exit(1);
			}
			
		} else {
			System.err.println("error: Missing stop id argument.");
			System.exit(1);
		}
		
		if (args.length >= 3){
			
			relative = args[2].equals("relative");
			if (!relative && !args[2].equals("absolute")){
				System.err.println("error: Time format must be 'relative' or 'absolute'.");
				System.exit(1);
			}
			
		}
		
		
		final CharSequence stopName = getStopName(id);
		if (stopName == null){
			System.err.println("Bus-stop '" + id + "' not found.");
			System.exit(1);
		}
		
		final HashMap<CharSequence,ArrayList<LocalDateTime>> arrivals = searchTrips(id, num);
		final HashMap<CharSequence,ArrayList<LocalDateTime>> routeArrivals = searchRoutes(arrivals, num);
		final HashMap<CharSequence,CharSequence> routeNames = searchRouteNames(routeArrivals.keySet());
		final LocalDateTime now = getNow();
		
		
		System.out.println("Bus-stop " + stopName);
		
		for (HashMap.Entry<CharSequence,ArrayList<LocalDateTime>> entry : routeArrivals.entrySet()){
			final CharSequence name = routeNames.getOrDefault(entry.getKey(), "?");
			final ArrayList<LocalDateTime> times = entry.getValue();
			
			System.out.print(" ");
			System.out.print(name);
			System.out.print(": ");
			
			for (int i = 0 ; i < times.size() ; i++){
				if (i > 0)
					System.out.print(", ");
				
				if (!relative){
					LocalTime t = times.get(i).toLocalTime();
					t = t.withSecond(0);
					System.out.print(t);
				} else {
					long min = Duration.between(now, times.get(i)).toMinutes();
					System.out.print(min);
					System.out.print(" min");
				}
				
			}
			
			System.out.println();
		}
		
	}
	
	
	static int[] getMapping(String header, String[] reqCols) throws Exception {
		int[] map = ColumParser.getMapping(header, reqCols);
		
		for (int i = 0 ; i < map.length ; i++){
			if (map[i] < 0)
				throw new Exception("Missing required data column '" + reqCols[i] + "'.");
		}
		
		return map;
	}
	
	
	static HashMap<CharSequence,ArrayList<LocalDateTime>> searchTrips(String id, int num) throws IOException, Exception {
		try (BufferedReader in = new BufferedReader(new FileReader(stopTimes_Path, StandardCharsets.UTF_8))){
			
			// Read header and discard BOM
			String header = in.readLine();
			if (header.charAt(0) == '\uFEFF'){
				header = header.substring(1);
			}
			
			final int[] map = getMapping(header, new String[]{"stop_id", "arrival_time", "trip_id"});
			final int ci_stopId = map[0];
			final int ci_arrival = map[1];
			final int ci_tripId = map[2];
			
			final LocalDateTime now = getNow();
			final LocalDate today = now.toLocalDate();
			final LocalDateTime maxTime = now.plusSeconds(maxLookahead.toSeconds());
			
			
			ColumParser col = new ColumParser();
			HashMap<CharSequence,ArrayList<LocalDateTime>> arrivals = new HashMap<>();
			
			while (true){
				String row = in.readLine();
				if (row == null){
					break;
				} else {
					col.setRow(row);
				}
				
				// Filter stop ids
				if (!col.eq(ci_stopId, id)){
					continue;
				}
				
				// Filter incompatible arrival time
				LocalDateTime arrival = null;
				try {
					arrival = today.atTime(LocalTime.parse(col.get(ci_arrival), timeFormat));
					
					// Wrap to next day
					if (arrival.isBefore(now)){
						arrival = arrival.plusDays(1);
					}
					
					if (!arrival.isBefore(maxTime)){
						continue;
					}
					
				} catch (Exception e){
					continue;
				}
				
				
				CharSequence trip = col.get(ci_tripId);
				
				ArrayList<LocalDateTime> l = arrivals.get(trip);
				if (l == null){
					l = new ArrayList<>(2);
					arrivals.put(trip, l);
				}
				
				if (l.size() < num){
					l.add(arrival);
					continue;
				}
				
				for (int i = 0 ; i < l.size() ; i++){
					if (arrival.isBefore(l.get(i))){
						l.remove(l.size() - 1);
						l.add(i, arrival);
						break;
					}
				}
				
			}
			
			return arrivals;
		}
	}
	
	
	static void merge(ArrayList<LocalDateTime> a, ArrayList<LocalDateTime> b, ArrayList<LocalDateTime> out, int max){
		int ia = 0;
		int ib = 0;
		
		while (out.size() < max){
			if (ia >= a.size()){
				if (ib < b.size())
					out.add(b.get(ib++));
				else
					break;
			}
			else if (ib >= b.size()){
				if (ia < a.size())
					out.add(a.get(ia++));
				else
					break;
			}
			else if (a.get(ia).isBefore(b.get(ib))){
				out.add(a.get(ia++));
			}
			else {
				out.add(b.get(ib++));
			}
		}
		
	}
	
	
	static HashMap<CharSequence,ArrayList<LocalDateTime>> searchRoutes(HashMap<CharSequence,ArrayList<LocalDateTime>> arrivals, int num) throws IOException, Exception {
		try (BufferedReader in = new BufferedReader(new FileReader(trips_path, StandardCharsets.UTF_8))){
			
			// Read header and discard BOM
			String header = in.readLine();
			if (header.charAt(0) == '\uFEFF'){
				header = header.substring(1);
			}
			
			final int[] map = getMapping(header, new String[]{"trip_id", "route_id"});
			final int ci_tripId = map[0];
			final int ci_routeId = map[1];
			
			ColumParser col = new ColumParser();
			HashMap<CharSequence,ArrayList<LocalDateTime>> routeArrivals = new HashMap<>();
			ArrayList<LocalDateTime> tmp = new ArrayList<>(num);
			
			while (true){
				String row = in.readLine();
				if (row == null){
					break;
				} else {
					col.setRow(row);
				}
				
				
				CharSequence trip = col.get(ci_tripId);
				ArrayList<LocalDateTime> tripArrivals = arrivals.get(trip);
				if (tripArrivals == null){
					continue;
				}
				
				CharSequence route = col.get(ci_routeId);
				if (route == null){
					continue;
				}
				
				
				ArrayList<LocalDateTime> l = routeArrivals.get(route);
				if (l == null){
					routeArrivals.put(route, new ArrayList<>(tripArrivals));
					continue;
				}
				
				tmp.clear();
				merge(tripArrivals, l, tmp, num);
				routeArrivals.put(route, tmp);
				tmp = l;
			}
			
			return routeArrivals;
		}
	}
	
	
	static HashMap<CharSequence,CharSequence> searchRouteNames(Set<CharSequence> routes) throws IOException, Exception {
		try (BufferedReader in = new BufferedReader(new FileReader(routes_path, StandardCharsets.UTF_8))){
			
			// Read header and discard BOM
			String header = in.readLine();
			if (header.charAt(0) == '\uFEFF'){
				header = header.substring(1);
			}
			
			final int[] map = getMapping(header, new String[]{"route_id", "route_short_name"});
			final int ci_routeId = map[0];
			final int ci_routeName = map[1];
			
			ColumParser col = new ColumParser();
			HashMap<CharSequence,CharSequence> names = new HashMap<>();
			
			while (true){
				String row = in.readLine();
				if (row == null){
					break;
				} else {
					col.setRow(row);
				}
				
				CharSequence route = col.get(ci_routeId);
				if (!routes.contains(route)){
					continue;
				}
				
				CharSequence name = col.get(ci_routeName);
				if (name == null){
					continue;
				}
				
				names.put(route, name);
			}
			
			return names;
		}
	}
	
	
	static CharSequence getStopName(String id) throws IOException, Exception {
		try (BufferedReader in = new BufferedReader(new FileReader(stops_path, StandardCharsets.UTF_8))){
			
			// Read header and discard BOM
			String header = in.readLine();
			if (header.charAt(0) == '\uFEFF'){
				header = header.substring(1);
			}
			
			final int[] map = getMapping(header, new String[]{"stop_id", "stop_name"});
			final int ci_stopId = map[0];
			final int ci_stopName = map[1];
			
			ColumParser col = new ColumParser();
			
			while (true){
				String row = in.readLine();
				if (row == null){
					break;
				} else {
					col.setRow(row);
				}
				
				if (col.eq(ci_stopId, id)){
					return col.get(ci_stopName);
				}
				
			}
			
			return null;
		}
	}
	
	
}