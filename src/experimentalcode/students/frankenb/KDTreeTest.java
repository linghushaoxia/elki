package experimentalcode.students.frankenb;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class KDTreeTest {

  @Test
  public void simpleTest() {
    List<NumberVector<?, ?>> items = new ArrayList<NumberVector<?, ?>>();
    items.add(new IntegerVector(new int[] { -4, -5 }));
    items.add(new IntegerVector(new int[] { -9, 3 }));
    items.add(new IntegerVector(new int[] { 9, -1 }));
    items.add(new IntegerVector(new int[] { 8, 6 }));
    items.add(new IntegerVector(new int[] { 6, -7 }));
    items.add(new IntegerVector(new int[] { -6, -5 }));
    items.add(new IntegerVector(new int[] { 3, 8 }));
    items.add(new IntegerVector(new int[] { 4, 1 }));
    items.add(new IntegerVector(new int[] { 7, -6 }));
    items.add(new IntegerVector(new int[] { 9, -3 }));
    Relation<NumberVector<?, ?>> dataSet = null; // new ListDataSet(2, items);

    KDTree tree = new KDTree(dataSet);

    KNNResult<DoubleDistance> list = tree.findNearestNeighbors(DBIDUtil.importInteger(1), 3, EuclideanDistanceFunction.STATIC);

    int counter = 0;
    assertEquals(3, list.size());
    for(DistanceResultPair<DoubleDistance> distanceEntry : list) {
      if(counter == 0) {
        assertEquals(1, distanceEntry.getDBID().getIntegerID());
      }
      else if(counter == 1) {
        assertEquals(5, distanceEntry.getDBID().getIntegerID());
      }
      else if(counter == 2) {
        assertEquals(0, distanceEntry.getDBID().getIntegerID());
      }
      counter++;
    }
  }

}