package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedHiCOPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.List;

/**
 * Provides the Correlation distance for real valued vectors.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PCABasedCorrelationDistanceFunction extends CorrelationBasedDistanceFunction {
  static {
    ASSOCIATION_ID = AssociationID.LOCAL_PCA;
    PREPROCESSOR_SUPER_CLASS = HiCOPreprocessor.class;
    DEFAULT_PREPROCESSOR_CLASS = KnnQueryBasedHiCOPreprocessor.class.getName();
    PREPROCESSOR_CLASS_D = "<class>the preprocessor to determine the the correlation dimension of the objects "
                           + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(PREPROCESSOR_SUPER_CLASS)
                           + ". (Default: " + DEFAULT_PREPROCESSOR_CLASS;
  }

  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "<double>a double specifying the threshold of a distance between a vector q and a given space that indicates that q adds a new dimension to the space (default is delta = " + DEFAULT_DELTA + ")";

  /**
   * The threshold of a distance between a vector q and a given space that
   * indicates that q adds a new dimension to the space.
   */
  private double delta;

  /**
   * Provides a CorrelationDistanceFunction with a pattern defined to accept
   * Strings that define an Integer followed by a separator followed by a
   * Double.
   */
  public PCABasedCorrelationDistanceFunction() {
    super();

    parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE, DELTA_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Sets the values for the parameters delta and preprocessor if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // delta
    if (optionHandler.isSet(DELTA_P)) {
      try {
        delta = Double.parseDouble(optionHandler.getOptionValue(DELTA_P));
        if (delta < 0) {
          throw new WrongParameterValueException(DELTA_P, optionHandler.getOptionValue(DELTA_P), DELTA_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(DELTA_P, optionHandler.getOptionValue(DELTA_P), DELTA_D, e);
      }
    }
    else {
      delta = DEFAULT_DELTA;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(DELTA_P, Double.toString(delta));

    return result;
  }

  /**
   * @see CorrelationBasedDistanceFunction#correlationDistance(de.lmu.ifi.dbs.data.RealVector, de.lmu.ifi.dbs.data.RealVector)
   */
  CorrelationDistance correlationDistance(RealVector dv1, RealVector dv2) {
    // TODO nur in eine Richtung?
    int dim = dv1.getDimensionality();

    // pca of rv1
    LocalPCA pca1 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv1.getID());
    Matrix v1 = pca1.getEigenvectors();
    Matrix v1_strong = pca1.strongEigenVectors();
    Matrix e1_czech = pca1.getSelectionMatrixOfStrongEigenvectors().copy();
    int lambda1 = pca1.getCorrelationDimension();
    // int lambda1 = 0;

    // pca of rv2
    LocalPCA pca2 = (LocalPCA) getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv2.getID());
    Matrix v2 = pca2.getEigenvectors();
    Matrix v2_strong = pca2.strongEigenVectors();
    Matrix e2_czech = pca2.getSelectionMatrixOfStrongEigenvectors();
    int lambda2 = pca2.getCorrelationDimension();
    // int lambda2 = 0;

    // for all strong eigenvectors of rv2
    Matrix m1_czech = v1.times(e1_czech).times(v1.transpose());
    for (int i = 0; i < v2_strong.getColumnDimension(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of rv1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transpose().times(v2_i).get(0, 0) - v2_i.transpose().times(m1_czech).times(v2_i).get(0, 0));

      // if so, insert v2_i into v1 and adjust v1
      // and compute m1_czech new, increase lambda1
      if (lambda1 < dim && dist > delta) {
        adjust(v1, e1_czech, v2_i, lambda1++);
        m1_czech = v1.times(e1_czech).times(v1.transpose());
      }
    }

    // for all strong eigenvectors of rv1
    Matrix m2_czech = v2.times(e2_czech).times(v2.transpose());
    for (int i = 0; i < v1_strong.getColumnDimension(); i++) {
      Matrix v1_i = v1_strong.getColumn(i);
      // check, if distance of v1_i to the space of rv2 > delta
      // (i.e., if v1_i spans up a new dimension)
      double dist = Math.sqrt(v1_i.transpose().times(v1_i).get(0, 0) - v1_i.transpose().times(m2_czech).times(v1_i).get(0, 0));

      // if so, insert v1_i into v2 and adjust v2
      // and compute m2_czech new , increase lambda2
      if (lambda2 < dim && dist > delta) {
        adjust(v2, e2_czech, v1_i, lambda2++);
        m2_czech = v2.times(e2_czech).times(v2.transpose());
      }
    }

    int correlationDistance = Math.max(lambda1, lambda2);

    // TODO delta einbauen
    // Matrix m_1_czech = v1.times(e1_czech).times(v1.transpose());
    // double dist_1 = normalizedDistance(rv1, rv2, m1_czech);
    // Matrix m_2_czech = v2.times(e2_czech).times(v2.transpose());
    // double dist_2 = normalizedDistance(rv1, rv2, m2_czech);
    // if (dist_1 > delta || dist_2 > delta) {
    // correlationDistance++;
    // }

    double euclideanDistance = euclideanDistance(dv1, dv2);
    return new CorrelationDistance(correlationDistance, euclideanDistance);
  }

  /**
   * Inserts the specified vector into the given orthonormal matrix
   * <code>v</code> at column <code>corrDim</code>. After insertion the
   * matrix <code>v</code> is orthonormalized and column
   * <code>corrDim</code> of matrix <code>e_czech</code> is set to the
   * <code>corrDim</code>-th unit vector..
   *
   * @param v       the orthonormal matrix of the eigenvectors
   * @param e_czech the selection matrix of the strong eigenvectors
   * @param vector  the vector to be inserted
   * @param corrDim the column at which the vector should be inserted
   */
  private void adjust(Matrix v, Matrix e_czech, Matrix vector, int corrDim) {
    int dim = v.getRowDimension();

    // set e_czech[corrDim][corrDim] := 1
    e_czech.set(corrDim, corrDim, 1);

    // normalize v
    Matrix v_i = vector.copy();
    Matrix sum = new Matrix(dim, 1);
    for (int k = 0; k < corrDim; k++) {
      Matrix v_k = v.getColumn(k);
      sum = sum.plus(v_k.times(v_i.scalarProduct(0, v_k, 0)));
    }
    v_i = v_i.minus(sum);
    v_i = v_i.times(1.0 / v_i.euclideanNorm(0));
    v.setColumn(corrDim, v_i);
  }

  /**
   * Computes the Euklidean distance between the given two vectors.
   *
   * @param dv1 first NumberVector
   * @param dv2 second NumberVector
   * @return the Euklidean distance between the given two vectors
   */
  private double euclideanDistance(RealVector dv1, RealVector dv2) {
    if (dv1.getDimensionality() != dv2.getDimensionality()) {
      throw new IllegalArgumentException(
      "Different dimensionality of NumberVectors\n  first argument: "
      + dv1.toString() + "\n  second argument: "
      + dv2.toString());
    }

    double sqrDist = 0;
    for (int i = 1; i <= dv1.getDimensionality(); i++) {
      double manhattanI = dv1.getValue(i).doubleValue() - dv2.getValue(i).doubleValue();
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }
}
