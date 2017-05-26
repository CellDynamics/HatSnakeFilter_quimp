package quimp.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.scijava.vecmath.Point2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.wsbc.quimp.plugin.ParamList;
import uk.ac.warwick.wsbc.quimp.plugin.QuimpPluginException;
import uk.ac.warwick.wsbc.quimp.utils.test.DataLoader;
import uk.ac.warwick.wsbc.quimp.utils.test.RoiSaver;

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
    testcase = new DataLoader(loadResource(getClass().getClassLoader(), tf).toString()).getData();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Load resource file from either jar or filesystem.
   * 
   * <p>If class loader is an object run from jar, this method will make binary copy of resource in
   * temporary folder and return path to it.
   * 
   * <p>This code is taken from
   * https://stackoverflow.com/questions/941754/how-to-get-a-path-to-a-resource-in-a-java-jar-file
   * 
   * @param c class loader
   * @param resource resource name and relative path
   * @return path to resource file
   */
  public static Path loadResource(ClassLoader c, String resource) {
    File file = null;
    URL res = c.getResource(resource);
    if (res.toString().startsWith("jar:")) {
      try {
        InputStream input = c.getResourceAsStream(resource);
        file = File.createTempFile(new Date().getTime() + "", "");
        OutputStream out = new FileOutputStream(file);
        int read;
        byte[] bytes = new byte[1024];

        while ((read = input.read(bytes)) != -1) {
          out.write(bytes, 0, read);
        }
        out.flush();
        out.close();
        input.close();
        file.deleteOnExit();
        return file.toPath();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      // this will probably work in your IDE, but not from a JAR
      return Paths.get(res.getFile());
    }
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
