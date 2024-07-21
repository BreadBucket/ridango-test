import java.util.ArrayList;
import java.util.Arrays;


public class ColumParser {
	private ArrayList<Integer> cols = new ArrayList<>(16);
	private String row;
	
	
	public ColumParser(){
		setRow("");
	}
	
	
	public int getStringIndex(int col){
		if (cols.size() <= col){
			int i = cols.getLast();
			
			// String deboog = row.substring(i);
			while (cols.size() <= col){
				i = row.indexOf(',', i);
				if (i < 0){
					return -1;
				}
				
				// deboog = row.substring(i+1);
				cols.add(++i);
			}
			
			return i;
		}
		
		return cols.get(col);
	}
	
	
	public CharSequence get(int col){
		int ca = getStringIndex(col);
		int cb = getStringIndex(col+1);
		
		if (ca < 0){
			return null;
		} else if (cb < 0){
			return row.subSequence(ca, row.length());
		}
		
		return row.subSequence(ca, cb-1);
	}
	
	
	public boolean eq(int col, String s){
		int ca = getStringIndex(col);
		int cb = getStringIndex(col+1) - 1;
		int len = cb - ca;
		
		if (ca < 0){
			return false;
		} else if (cb < 0){
			len = row.length() - ca;
		}
		
		return row.regionMatches(ca, s, 0, len);
	}
	
	
	public void setRow(String row){
		this.row = (row != null) ? row : "";
		cols.clear();
		cols.add(0);
	}
	
	
	public static int[] getMapping(String header, String[] colums){
		if (header == null){
			return null;
		} else if (colums == null || colums.length <= 0){
			return new int[0];
		}
		
		int[] map = new int[colums.length];
		Arrays.fill(map, -1);
		
		int slots = map.length;
		int col = 0;
		int ci = 0;
		
		// String debug = header;
		while (true){
			final int next = header.indexOf(',', ci);
			final int len = (next < 0) ? header.length() - ci : next - ci;
			
			for (int i = 0; i < map.length; i++){
				if (map[i] < 0 && colums[i].length() == len && header.regionMatches(true, ci, colums[i], 0, len)){
					map[i] = col;
					
					if (--slots <= 0){
						return map;
					}
					
					break;
				}
			}
			
			if (next < 0){
				break;
			}
			
			ci = next + 1;
			col++;
			// debug = header.substring(ci);
		}
		
		return map;
	}
	
	
}
