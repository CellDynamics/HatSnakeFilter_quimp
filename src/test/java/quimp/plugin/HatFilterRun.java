package quimp.plugin;

import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.scijava.vecmath.Point2d;

import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;

/**
 * Test class for HatFilter UI.
 * 
 * <p>Shows UI for HatFilter
 * 
 * @author p.baniukiewicz
 *
 */
public class HatFilterRun {

  /**
   * @param args
   * @throws QuimpPluginException
   * @throws InterruptedException
   */
  @SuppressWarnings("serial")
  public static void main(String[] args) throws QuimpPluginException, InterruptedException {
    List<Point2d> input;
    // test data
    input = new ArrayList<>();
    input.add(new Point2d(923, 700));
    input.add(new Point2d(577.5, 1175));
    input.add(new Point2d(18, 993));
    input.add(new Point2d(18, 406));
    input.add(new Point2d(577, 224));
    // input.add(new Vector2d( 428, -4.87));
    // input.add(new Vector2d( 3.11, -3.9));

    // create instance of hatfilter
    HatFilterInst hf = new HatFilterInst();
    hf.attachData(input);
    hf.setPluginConfig(new ParamList() {
      {
        put("window", "3");
        put("pnum", "1");
        put("alevmin", "0.0"); // case insensitive
        put("alevmax", "1.0"); // case insensitive
      }
    });
    CountDownLatch startSignal = new CountDownLatch(1);
    // hf.pluginWnd1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // hf.pluginWnd1.addWindowListener(new WindowAdapter() {
    // @Override
    // // This method will be called when BOA_ window is closed
    // public void windowClosing(WindowEvent arg0) {
    // startSignal.countDown(); // decrease latch by 1
    // }
    // });
    hf.showUi(true);
    // hf.pluginWnd.setVisible(true); // show window
    // main thread waits here until Latch reaches 0
    startSignal.await();
  }
}

/**
 * Wrapper for HatFilter that allows to access private member pluginWnd
 * 
 * @author p.baniukiewicz
 *
 */
class HatFilterInst extends HatSnakeFilter_ {

  public HatFilterInst() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see quimp.plugin.HatSnakeFilter_#buildWindow(com.github.celldynamics.quimp.plugin.ParamList)
   */
  @Override
  public void buildWindow(ParamList def) {
    super.buildWindow(def);
    WindowListener[] wl = pluginWnd.getWindowListeners();
    pluginWnd.removeWindowListener(wl[wl.length - 1]); // this caused cleaning points stored
  }

}