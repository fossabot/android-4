// Quick debug to see actual grid reference output
import uk.trigpointing.android.types.LatLon;

public class DebugTest {
    public static void main(String[] args) {
        LatLon location = new LatLon(54.5270, -3.0165);
        String gridRef6 = location.getOSGB6();
        String gridRef10 = location.getOSGB10();
        
        System.out.println("Grid ref 6: '" + gridRef6 + "' length: " + gridRef6.length());
        System.out.println("Grid ref 10: '" + gridRef10 + "' length: " + gridRef10.length());
    }
}
