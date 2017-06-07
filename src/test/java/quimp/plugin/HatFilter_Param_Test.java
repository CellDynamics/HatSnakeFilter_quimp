package quimp.plugin;

import static com.github.baniuk.ImageJTestSuite.dataaccess.ResourceLoader.loadResource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.scijava.vecmath.Point2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.baniuk.ImageJTestSuite.dataaccess.DataLoader;
import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.utils.test.RoiSaver;

/**
 * Parameterised test for HatFilter
 * 
 * Generates images of processed data as well as images of original data. Those can be viewed in
 * <EM>../src/test/resources/HatFilter.m</EM>
 * 
 * @author p.baniukiewicz
 *
 */
@RunWith(Parameterized.class)
public class HatFilter_Param_Test {
  static final Logger LOGGER = LoggerFactory.getLogger(HatFilter_Param_Test.class.getName());
  /**
   * The tmpdir.
   */
  static String tmpdir = System.getProperty("java.io.tmpdir") + File.separator;
  private Integer window;
  private Integer pnum;
  private Double alev;
  private List<Point2d> testcase;
  private Path testfileName;

  /**
   * Parameterized constructor.
   * 
   * Each parameter should be placed as an argument here Every time runner triggers, it will pass
   * the arguments from parameters we defined to this method
   * 
   * @param testFileName test file name
   * @param window filter window size
   * @param pnum number of protrusions to find
   * @param alev acceptance level
   * @see DataLoader
   * @see HatSnakeFilter_
   */
  public HatFilter_Param_Test(String testFileName, Integer window, Integer pnum, Double alev) {
    this.testfileName = Paths.get(testFileName);
    this.window = window;
    this.pnum = pnum;
    this.alev = alev;
  }

  /**
   * Called after construction but before tests
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    String tf = testfileName.getFileName().toString();
    testcase = new DataLoader(loadResource(getClass().getClassLoader(), tf).toString())
            .getListofPoints();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Set of parameters for tests.
   * 
   * @return List of strings with paths to testfiles and smooth parameter
   * @see <a href="creating *.dat files">QuimP-toolbox/Prototyping/59-Shape_filtering/main.m</a>
   */
  @Parameterized.Parameters
  public static Collection<Object[]> testFiles() {
    return Arrays.asList(
            new Object[][] { { "testData_137.dat", 23, 1, 0.0 }, { "testData_1.dat", 23, 1, 0.0 },
                { "testData_125.dat", 23, 1, 0.0 }, { "testData_75.dat", 23, 1, 0.0 },
                { "testData_137.dat", 23, 2, 0.0 }, { "testData_1.dat", 23, 2, 0.0 },
                { "testData_125.dat", 23, 2, 0.0 }, { "testData_75.dat", 23, 2, 0.0 } });
  }

  /**
   * Test of getInterpolationLoess method
   * 
   * Pre: Real cases extracted from
   * 
   * Post: Save image test_HatFilter_* in /tmp/
   * 
   * @throws QuimpPluginException
   * @see <a
   *      href="verification of logs (ratios, indexes, etc)">QuimP-toolbox/Prototyping/59-Shape_filtering/HatFilter.m</a>
   * @see <a href="resorces">/src/test/resources/HatFilter.m</a>
   * @see <a href="creating *.dat files">QuimP-toolbox/Prototyping/59-Shape_filtering/main.m</a>
   */
  @SuppressWarnings("serial")
  @Test
  public void test_HatFilter() throws QuimpPluginException {
    List<Point2d> out;
    HatSnakeFilter_ hf = new HatSnakeFilter_();
    hf.attachData(testcase);
    hf.setPluginConfig(new ParamList() {
      {
        put("window", String.valueOf(window));
        put("pnum", String.valueOf(pnum));
        put("alev", String.valueOf(alev));
      }
    });
    out = hf.runPlugin();
    RoiSaver.saveRoi(tmpdir + "test_HatFilter_" + testfileName.getFileName() + "_"
            + window.toString() + "_" + pnum.toString() + "_" + alev.toString() + ".tif", out);
    LOGGER.debug("setUp: " + testcase.toString());
  }

  /**
   * Simple test of RoiSaver class, create reference images without processing but with the same
   * name scheme as processed data.
   * 
   * Post: Save image in /tmp
   * 
   * @see <a href="resorces">/src/test/resources/HatFilter.m</a>
   */
  @Test
  public void test_roiSaver() {
    RoiSaver.saveRoi(tmpdir + "ref_HatFilter_" + testfileName.getFileName() + "_"
            + window.toString() + "_" + pnum.toString() + "_" + alev.toString() + ".tif", testcase);
  }
}
