package quimp.plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Tuple2d;
import org.scijava.vecmath.Vector2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.PropertyReader;
import com.github.celldynamics.quimp.ViewUpdater;
import com.github.celldynamics.quimp.geom.BasicPolygons;
import com.github.celldynamics.quimp.plugin.IQuimpPluginSynchro;
import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.plugin.snakes.IQuimpBOAPoint2dFilter;
import com.github.celldynamics.quimp.plugin.utils.IPadArray;
import com.github.celldynamics.quimp.plugin.utils.QWindowBuilder;

/**
 * Implementation of HatFilter for removing convexes from polygon.
 * 
 * <h3>List of user parameters:</h3>
 * <ol>
 * <li><i>window</i> - Size of window in pixels. It is responsible for sensitivity to protrusions of
 * given size. Larger window can eliminate small and large protrusions whereas smaller window is
 * sensitive only to small protrusions.
 * <li>window should be from 3 to number of outline points.
 * <li><i>pnum</i> - Number of protrusions that will be found in outline. If not limited by
 * <i>alev</i> parameter the algorithm will eliminate <i>pnum</i> objects from outline without
 * considering if they are protrusions or not.
 * <li><i>pnum</i> should be from 1 to any value. Algorithm stops searching when there is no
 * candidates to remove.
 * <li><i>alev</i> - Threshold value, if circularity computed for given window position is lower
 * than threshold this window is not eliminated regarding <i>pnum</i> or its rank in circularities.
 * <li><i>alev</i> should be in range form 0 to 1, where 0 stands for accepting every candidate
 * </ol>
 * <p>
 * <h3>General description of algorithm:</h3> The window slides over the wrapped contour. Points
 * inside window for its position \a p are considered as candidates to removal from contour if they
 * meet the following criterion:
 * <ol>
 * <li>The window has achieved for position \a p circularity parameter <i>c</i> larger than
 * <i>alev</i>
 * <li>The window on position <i>p</i> does not touch any other previously found window.
 * <li>Points of window <i>p</i> are convex.
 * </ol>
 * <p>
 * Every window <i>p</i> has assigned a <i>rank</i>. Bigger <i>rank</i> stands for better candidate
 * to remove. Algorithm tries to remove first <i>pnum</i> windows (those with biggest ranks) that
 * meet above rules.
 * <p>
 * <H3>Detailed description of algorithm</H3> The algorithm comprises of three main steps:
 * <ol>
 * <li>Preparing <i>rank</i> table of candidates to remove
 * <li>Iterating over <i>rank</i> table to find <i>pnum</i> such candidates who meet rules and store
 * their coordinates in <i>ind2rem</i> array. By candidates it is understood sets of polygon indexes
 * that is covered by window on given position. For simplification those vertexes are identified by
 * lover and upper index of window in outline array (input).
 * <li>Forming output table without protrusions.
 * </ol>
 * <p>
 * <H2>First step</H2> The window of size <i>window</i> slides over looped data. Looping is
 * performed by java.util.Collections.rotate method that shift data left copying falling out indexes
 * to end of the set (finally the window is settled in constant position between indexes
 * <0;window-1>). For each its position <i>r</i> the candidate points are deleted from original
 * contour and circularity is computed (see {@link #getCircularity(List)}). Then candidate points
 * are passed to {@link #getWeighting(List)} method where weight is evaluated. The role of weight is
 * to promote in <i>rank</i> candidate points that are cumulated in small area over distributed
 * sets. Thus weight should give larger values for that latter distribution than for cumulated one.
 * Currently weights are calculated as standard deviation of distances of all candidate points to
 * center of mass of these points (or mean point if polygon is invalid). Finally circularity(r) is
 * divided by weight (<i>r</i>) and stored in <i>circ</i> array. Additionally in this step the
 * convex is checked. All candidate points are tested for inclusion in contour without these points.
 * This information is stored in <i>convex</i> array. Finally rank array <i>circ</i> is normalised
 * to maximum element.
 * <p>
 * <H2>Second step</H2> In second step array of ranks <i>circ</i> is sorted in descending order. For
 * every rank in sorted table the real position of window is retrieved (that gave this rank). The
 * window position is defined here by two numbers - lower and upper range of indexes covered
 * by it. The candidate points from this window are validated for criterion:
 * <ol>
 * <li><i>rank</i> must be greater than <i>alev</i>
 * <li>lower and upper index of window (index means here number of polygon vertex in array) must not
 * be included in any previously found window. This checking is done by deriving own class
 * WindowIndRange with overwritten {@link WindowIndRange#compareTo(Object)} method that defines
 * rules of equality and non relations between ranges. Basically any overlapping range or included
 * is considered as equal and rejected from storing in <i>ind2rem</i> array.
 * <li>candidate points must be convex. As mentioned before <i>convex</i> means that <b>all</b>
 * candidate points are outside the original contour formed without these points.
 * <li>current <i>rank</i> (<i>circ</i>) is greater than <i>alev</i>
 * </ol>
 * If all above criterion are meet the window [lower;upper] is stored in <i>ind2rem</i>. Windows on
 * end of
 * data are wrapped by dividing them for two sub-windows: [w;end] and [0;c] otherwise they may cover
 * the whole range (e.g. <10;3> does not stand for window from 10 wrapped to 3 but window from 3 to
 * 10).
 * 
 * <p>The second step is repeated until pnum object will be found or end of candidates will be
 * reached.
 * 
 * <p><H2>Third step</H2> In third step every point from original contour is tested for including in
 * array <i>ind2rem</i> that contains ranges of indexes to remove. Points on index that is not
 * included in any of ranges stored in <i>ind2rem</i> are copied to output.
 * 
 * @author p.baniukiewicz
 */
public class HatSnakeFilter_ extends QWindowBuilder implements IQuimpBOAPoint2dFilter, IPadArray,
        ChangeListener, ActionListener, IQuimpPluginSynchro {
  static final Logger LOGGER = LoggerFactory.getLogger(HatSnakeFilter_.class.getName());
  private final int drawSize = 200; // size of draw area in window

  private int window; // filter's window size
  private int pnum; // how many protrusions to remove
  private double alev; // minimal acceptance level
  private List<Point2d> points; // original contour passed from QuimP
  private ParamList uiDefinition; // Definition of UI for this plugin
  private DrawPanel dp; // Here we will draw. This panel is plot in place of help field
  private ExPolygon snakePolygon; // representation of snake as polygon
  private ExPolygon pout; // output polygon based on \c out
  private List<Point2d> out; // output after filtering
  private JTextArea logArea;
  private int err; // general counter of log entries
  private ViewUpdater qcontext; // remember QuimP context to recalculate and update its view

  /**
   * Construct HatFilter Input array with data is virtually circularly padded.
   */
  public HatSnakeFilter_() {
    LOGGER.trace("Entering constructor");
    this.window = 15;
    this.pnum = 1;
    this.alev = 0;
    LOGGER.debug("Set default parameter: window=" + window + " pnum=" + pnum + " alev=" + alev);
    // create UI using QWindowBuilder
    uiDefinition = new ParamList(); // will hold ui definitions
    // configure window, names of UI elements are also names of variables
    // exported/imported by set/getPluginConfig
    uiDefinition.put("name", "HatFilter"); // name of window
    uiDefinition.put("window", "spinner: 3: 51: 2:" + Integer.toString(window));
    uiDefinition.put("pnum", "spinner: 1: 6: 1:" + Integer.toString(pnum));
    uiDefinition.put("alev", "spinner: 0: 1:0.01:" + Double.toString(alev));
    buildWindow(uiDefinition); // construct ui (not shown yet)
    points = null; // not attached yet
    pout = null; // not calculated yet
    err = 1; // first line in log window
  }

  /**
   * Attach data to process.
   * 
   * <p>Data are as list of vectors defining points of polygon. Passed points should be sorted
   * according to a clockwise or anti-clockwise direction
   * 
   * @param data Polygon points (can be null)
   */
  @Override
  public void attachData(List<Point2d> data) {
    LOGGER.trace("Entering attachData");
    points = data;
    pout = null; // delete any processed polygon
    if (points == null) {
      LOGGER.info("No data attached");
      return;
    }
    snakePolygon = new ExPolygon(data); // create polygon from points
    snakePolygon.fitPolygon(drawSize); // adjust its size to draw window

  }

  /**
   * Main filter runner.
   * 
   * <p>This version assumes that user clicked Apply button to populate data from UI to plugin or
   * any
   * other ui element. User can expect that points will be always valid but they optionally may
   * have 0 length.
   * 
   * @return Processed input list, size of output list may be different than input. Empty output
   *         is also allowed.
   * @see #actionPerformed(ActionEvent)
   * @see #stateChanged(ChangeEvent)
   */
  @Override
  public List<Point2d> runPlugin() throws QuimpPluginException {
    // internal parameters are not updated here but when user click apply
    LOGGER.info(String.format("Run plugin with params: window %d, pnum %d, alev %f", window, pnum,
            alev));

    BasicPolygons bp = new BasicPolygons(); // provides geometry processing
    // output table for plotting temporary results of filter
    List<Point2d> out = new ArrayList<Point2d>();
    // check input conditions
    if (window % 2 == 0 || window < 0) {
      throw new QuimpPluginException("Window must be uneven, positive and larger than 0");
    }
    if (window >= points.size()) {
      throw new QuimpPluginException("Processing window to long");
    }
    if (window < 3) {
      throw new QuimpPluginException("Window should be larger than 2");
    }
    if (pnum <= 0) {
      throw new QuimpPluginException("Number of protrusions should be larger than 0");
    }
    if (alev < 0) {
      throw new QuimpPluginException("Acceptacne level should be positive");
    }
    // temporary variable for keeping window currently tested for containing in ind2rem
    WindowIndRange indexTest = new WindowIndRange();
    // Step 1 - Build circularity table
    // array to store circularity for window positions. Index is related to window position
    // (negative shift in rotate)
    ArrayList<Double> circ = new ArrayList<Double>();
    // store information if points for window at r position are convex compared to shape without
    // these points
    ArrayList<Boolean> convex = new ArrayList<Boolean>();

    double tmpCirc;
    for (int r = 0; r < points.size(); r++) {
      LOGGER.trace("------- Iter: " + r + "-------");
      LOGGER.trace("points: " + points.toString());
      // get all points except window. Window has constant position 0 - (window-1)
      List<Point2d> pointsnowindow = points.subList(window, points.size());
      LOGGER.trace("sub: " + pointsnowindow.toString());
      tmpCirc = getCircularity(pointsnowindow);
      LOGGER.trace("circ " + tmpCirc);
      // calculate weighting for circularity
      List<Point2d> pointswindow = points.subList(0, window); // get points for window only
      LOGGER.trace("win: " + pointswindow.toString());
      tmpCirc /= getWeighting(pointswindow); // calculate weighting for window content
      LOGGER.trace("Wcirc " + tmpCirc);
      circ.add(tmpCirc); // store weighted circularity for shape without window
      // check if points of window are convex according to shape without these points
      convex.add(bp.areAllPointsInside(pointsnowindow, pointswindow)); // true if concave
      LOGGER.trace("con: " + convex.get(convex.size() - 1));
      // move window to next position
      Collections.rotate(points, -1); // rotates by -1 what means that on first n positions
                                      // of points there are different values simulate window
                                      // first iter 0 1 2 3 4 5... (w=[0 1 2])
                                      // second itr 1 2 3 4 5 0... (w=[1 2 3])
                                      // last itera 5 0 1 2 3 4... (w=[5 0 1])

    }
    // normalize circularity to 1
    double maxCirc = Collections.max(circ);
    for (int r = 0; r < circ.size(); r++) {
      circ.set(r, circ.get(r) / maxCirc);
    }

    // Step 2 - Check criterion for all windows
    TreeSet<WindowIndRange> ind2rem = new TreeSet<>(); // <lower;upper> range of indexes to remove
    // need sorted but the old one as well to identify windows positions
    ArrayList<Double> circsorted = new ArrayList<>(circ);
    circsorted.sort(Collections.reverseOrder()); // sort in descending order
    LOGGER.trace("cirs: " + circsorted.toString());
    LOGGER.trace("circ: " + circ.toString());

    if (circsorted.get(0) < alev) {
      return points; // just return non-modified data;
    }

    int found = 0; // how many protrusions we have found already
    // current index in circsorted - number of window to analyze. Real position of
    // window on data can be retrieved by finding value from sorted array circularityin non sorted,
    // where order of data is related to window position starting from 0 for most left point of
    // window
    int i = 0;
    boolean contains; // temporary result of test if current window is included in any prev
    while (found < pnum) { // do as long as we find pnum protrusions (or to end of candidates)
      if (i >= circsorted.size()) { // no more data to check, probably we have less prot. pnum
        LOGGER.warn("Can find next candidate. Use smaller window");
        break;
      }
      if (found > 0) {
        if (circsorted.get(i) < alev) { // if ith circularity smaller than limit
          break;
        } // stop searching because all i+n are smaller as well
        // find where it was before sorting and store in window positions
        int startpos = circ.indexOf(circsorted.get(i));
        // check if we already have this index in list indexes to remove
        if (startpos + window - 1 >= points.size()) { // if at end, we must turn to begin
          indexTest.setRange(startpos, points.size() - 1); // to end
          contains = ind2rem.contains(indexTest); // beginning of window at the end of dat
          indexTest.setRange(0, window - (points.size() - startpos) - 1); // turn to start
          contains &= ind2rem.contains(indexTest); // check rotated part at beginning
        } else {
          indexTest.setRange(startpos, startpos + window - 1);
          contains = ind2rem.contains(indexTest);
        }
        // this window doesnt overlap with those ound already and it is convex
        if (!contains && !convex.get(startpos)) {
          // store range of indexes that belongs to window
          if (startpos + window - 1 >= points.size()) { // as prev split to two windows
            // if we are on the end of data
            ind2rem.add(new WindowIndRange(startpos, points.size() - 1));
            // turn window to beginning
            ind2rem.add(new WindowIndRange(0, window - (points.size() - startpos) - 1));
          } else {
            ind2rem.add(new WindowIndRange(startpos, startpos + window - 1));
          }
          LOGGER.trace("added win for i=" + i + " startpos=" + startpos + " coord:"
                  + points.get(startpos).toString());
          found++;
          i++;
        } else { // go to next candidate in sorted circularities
          i++;
        }
      } else { // first candidate always accepted
        // find where it was before sorting and store in window positions
        int startpos = circ.indexOf(circsorted.get(i));
        // store range of indexes that belongs to window
        if (startpos + window - 1 >= points.size()) { // as prev split to two windows
          // if we are on the end of data
          ind2rem.add(new WindowIndRange(startpos, points.size() - 1));
          // turn window to beginning
          ind2rem.add(new WindowIndRange(0, window - (points.size() - startpos) - 1));
        } else {
          ind2rem.add(new WindowIndRange(startpos, startpos + window - 1));
        }
        LOGGER.trace("added win for i=" + i + " startpos=" + startpos + " coord:"
                + points.get(startpos).toString());
        i++;
        found++;
      }
    }
    LOGGER.trace("winpos: " + ind2rem.toString());
    // Step 3 - remove selected windows from input data
    // array will be copied to new one skipping points to remove
    for (i = 0; i < points.size(); i++) {
      // set upper and lower index to the same value - allows to test particular index for its
      // presence in any defined range
      indexTest.setSame(i);
      if (!ind2rem.contains(indexTest)) { // check if any window position (lower and upper bound)
        out.add(new Point2d(points.get(i)));
      } // include tested point. Copy it to new array if not
    }
    return out;
  }

  /**
   * Calculate circularity of polygon.
   * 
   * <p>Circularity is computed as: \f[ circ=\frac{4*\pi*A}{P^2} \f] where \f$A\f$ is polygon area
   * and \f$P\f$ is its perimeter
   * 
   * @param p Polygon vertices
   * @return circularity
   */
  private double getCircularity(final List<? extends Tuple2d> p) {
    double area;
    double perim;
    BasicPolygons b = new BasicPolygons();
    area = b.getPolyArea(p);
    perim = b.getPolyPerim(p);

    return (4 * Math.PI * area) / (perim * perim);
  }

  /**
   * Calculates weighting based on distribution of window points.
   * 
   * <p>Calculates center of mass of window points and then standard deviations of lengths between
   * this point and every other point. Cumulated distributions like protrusions give smaller
   * values than elongated ones.
   * 
   * <p>If input polygon <i>snakePolygon</i> (which is only part of whole cell shape) is defective,
   * i.e
   * its
   * edges cross, the weight is calculated using middle vector defined as mean of coordinates.
   * 
   * @param snakePolygon Polygon vertices
   * @return Weight
   */
  private double getWeighting(final List<Point2d> p) {
    double[] len = new double[p.size()];
    BasicPolygons bp = new BasicPolygons();
    Vector2d middle;
    try { // check if input polygon is correct
      middle = new Vector2d(bp.polygonCenterOfMass(p));
    } catch (IllegalArgumentException e) { // if not get middle point as mean
      double mx = 0;
      double my = 0;
      for (Point2d v : p) {
        mx += v.x;
        my += v.y;
      }
      middle = new Vector2d(mx / p.size(), my / p.size());
    }
    int i = 0;
    // get lengths
    for (Point2d v : p) {
      Vector2d vec = new Vector2d(middle); // vector between px and middle
      vec.sub(v);
      len[i++] = vec.length();
    }
    // get mean
    double mean = 0;
    for (double d : len) {
      mean += d;
    }
    mean /= p.size();
    // get std
    double std = 0;
    for (double d : len) {
      std += Math.pow(d - mean, 2.0);
    }
    std /= points.size();
    std = Math.sqrt(std);

    LOGGER.debug("w " + std);
    return std;
  }

  /**
   * This method should return a flag word that specifies the filters capabilities.
   * 
   * @return Configuration codes
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin
   */
  @Override
  public int setup() {
    LOGGER.trace("Entering setup");
    return DOES_SNAKES + CHANGE_SIZE;
  }

  /**
   * Configure plugin and overrides default values.
   * 
   * Supported keys:
   * <ol>
   * <li><i>window</i> - size of main window
   * <li><i>crown</i> - size of inner window
   * <li><i>sigma</i> - cut-off value (see class description)
   * </ol>
   * 
   * @param par configuration as pairs [key,val]. Keys are defined by plugin creator and plugin
   *        caller do not modify them.
   * @throws QuimpPluginException on wrong parameters list or wrong parameter conversion
   * @see com.github.celldynamics.quimp.plugin.IQuimpPlugin#setPluginConfig(ParamList)
   */
  @Override
  public void setPluginConfig(final ParamList par) throws QuimpPluginException {
    try {
      window = par.getIntValue("window");
      pnum = par.getIntValue("pnum");
      alev = par.getDoubleValue("alev");
      setValues(par); // copy incoming parameters to UI
    } catch (Exception e) {
      // we should never hit this exception as parameters are not touched by caller they are
      // only passed to configuration saver and restored from it
      throw new QuimpPluginException("Wrong input argument->" + e.getMessage(), e);
    }
  }

  /**
   * Transfer plugin configuration to QuimP
   * 
   * <p>Only parameters mapped to UI by QWindowBuilder are supported directly by getValues() Any
   * other parameters created outside QWindowBuilder should be added here manually.
   */
  @Override
  public ParamList getPluginConfig() {
    return getValues();
  }

  @Override
  public int showUi(boolean val) {
    LOGGER.trace("Got message to show UI");
    if (toggleWindow(val) == true) {
      recalculatePlugin();
    }
    return 0;
  }

  /**
   * Plugin version.
   */
  @Override
  public String getVersion() {
    String trimmedClassName = getClass().getSimpleName();
    trimmedClassName = trimmedClassName.substring(0, trimmedClassName.length() - 1); // no _
    // _ at the end of class does not appears in final jar name, we need it to
    // distinguish between plugins
    return PropertyReader.readProperty(getClass(), trimmedClassName,
            "quimp/plugin/plugin.properties", "internalVersion");
  }

  /**
   * Override of uk.ac.warwick.wsbc.plugin.utils.QWindowBuilder.BuildWindow()
   * 
   * <p>The aim is to: 1) attach listeners for spinners for preventing even numbers 2) attach
   * listener for build-in apply button 3) add draw field DrawPanel
   */
  @Override
  public void buildWindow(final ParamList def) {
    super.buildWindow(def); // window must be built first

    // attach listeners to ui to update window on new parameters
    ((JSpinner) ui.get("Window")).addChangeListener(this); // attach listener to selected ui
    ((JSpinner) ui.get("pnum")).addChangeListener(this); // attach listener to selected ui
    ((JSpinner) ui.get("alev")).addChangeListener(this); // attach listener to selected ui
    applyB.addActionListener(this); // attach listener to apply button
    // in place of CENTER pane in BorderLayout layout from super.BuildWindow
    // we create few extra controls
    GridLayout gc = new GridLayout(2, 1, 5, 5);
    Panel jp = new Panel(); // panel in CENTER pane
    jp.setLayout(gc);

    dp = new DrawPanel(); // create drawable JFrame
    jp.add(dp);

    Panel jp1 = new Panel(); // subpanel for two text fields
    jp1.setLayout(gc);

    // help is supported by QBuildWindow but here we moved it on different than default position
    JTextArea helpArea = new JTextArea(); // default size of text area

    helpArea.setEditable(false);
    helpArea.setText(
            "About plugin\n" + "Click Apply to update main view. Preview shows only last selected"
                    + " object and update it on any change of plugin parameters");
    helpArea.setLineWrap(true); // with wrapping

    logArea = new JTextArea(); // default size of text area
    JScrollPane logPanel = new JScrollPane(logArea);
    logArea.setEditable(false);
    logArea.setText("Log:\n"); // set help text
    logArea.setLineWrap(true); // with wrapping

    jp1.add(new JScrollPane(helpArea));
    jp1.add(logPanel);
    jp.add(jp1);

    pluginPanel.add(jp, BorderLayout.CENTER); // add in center position (in place of help zone)
    pluginWnd.pack();
    pluginWnd.addWindowListener(new HatWindowAdapter()); // add listener for winodw activation
  }

  /**
   * React on spinners changes.
   */
  @Override
  public void stateChanged(ChangeEvent ce) {
    // prevent applying default values before setPluginConfig is used because method is called on
    // window creation
    if (isWindowVisible() == true) {
      recalculatePlugin();
    }
  }

  /**
   * React on Apply button.
   * 
   * <p>Here Apply button copies window content into plugin structures. This is different approach
   * than in LoessFilter and MeanFilter where window content was copied while {@link #runPlugin()}
   * command
   * 
   * <p>This button run plugin and creates preview of filtered data
   * 
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object b = e.getSource();
    if (b == applyB) { // pressed apply, copy ui data to plugin
      // order of these two is important because updateView() externally run the whole
      // plugin and reconnects external data what updates preview and delete any recalculated
      // result.
      qcontext.updateView(); // run whole plugin from BOA context
      recalculatePlugin(); // transfers data from ui to plugin and plot example on screen
    }
  }

  /**
   * Recalculate plugin on every change of its parameter.
   * 
   * <p>Used only for previewing. Repaint window as well
   */
  private void recalculatePlugin() {
    // check if we have correct data
    if (points == null) {
      LOGGER.warn("No data attached");
      return;
    }
    // transfer data from ui
    window = getIntegerFromUI("window");
    pnum = getIntegerFromUI("pnum");
    alev = getDoubleFromUI("alev");
    LOGGER.debug(String.format("Updated from UI: window %d, pnum %d, alev %f", window, pnum, alev));
    // run plugin for set parameters
    try {
      out = runPlugin(); // may throw if no data attached this is inly to get preview
      pout = new ExPolygon(out); // create new figure from out data
      // fit to size from original polygon,
      pout.fitPolygon(drawSize, snakePolygon.initbounds, snakePolygon.scale);
      // modified one will be centered to
      // original one
      dp.repaint(); // repaint window
    } catch (QuimpPluginException e1) { // ignore exception in general
      LOGGER.error(e1.toString());
      logArea.append("#" + err + ": " + e1.getMessage() + '\n');
      err++;
    }
  }

  /**
   * Class for plotting in center part of plugin derived from QWindowBuilder
   * 
   * @author p.baniukiewicz
   *
   */
  class DrawPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    DrawPanel() {
      setPreferredSize(new Dimension(drawSize, drawSize));
    }

    /**
     * Main plotting function.
     * 
     * <p>Plots two polygons, original and processed in red
     */
    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      g.setColor(Color.BLACK);
      g.fillRect(0, 0, drawSize, drawSize);
      g.setColor(Color.WHITE);
      if (points != null) {
        g.drawPolygon(snakePolygon);
      }
      if (pout != null) { // pout is initialized after first apply click
        g.setColor(Color.RED);
        g.drawPolygon(pout); // draw output polygon (processed)
      }
      if (points == null || pout == null) { // clera if there is no valid data
        g.setColor(Color.BLACK);
        g.clearRect(0, 0, drawSize, drawSize);
        g.fillRect(0, 0, drawSize, drawSize);
        g.setColor(Color.WHITE);
      }

    }
  }

  @Override
  public void attachContext(ViewUpdater b) {
    qcontext = b;
  }

  @Override
  public String about() {
    return "Delete convexity from outline.\n" + "Author: Piotr Baniukiewicz\n"
            + "mail: p.baniukiewicz@warwick.ac.uk";
  }

  /**
   * Add action on window focus
   * 
   * <p>This is used for updating preview screen in plugin. When window became in focus, last snake
   * stored in ViewUpdater is gathered. This snake is updated on every action in BOA. Data from
   * ViewUpdater may be null if user deleted Snake. If there is more snakes on screen after
   * deleting Snake id=n, active became Snake id=n-1
   * 
   * @author p.baniukiewicz
   *
   */
  class HatWindowAdapter extends WindowAdapter {
    @Override
    public void windowActivated(WindowEvent e) {
      points = qcontext.getSnakeasPoints(); // get last snake from ViewUpdater
      if (points != null) {
        snakePolygon = new ExPolygon(points); // create polygon from points
        snakePolygon.fitPolygon(drawSize); // adjust its size to draw window
        recalculatePlugin();
      } else { // if data from ViewUpdater are null, invalidate output as well and clear view
        pout = null;
        dp.repaint();
      }
      super.windowActivated(e);
    }
  }

  /**
   * Helper class supporting scaling and fitting polygon to DrawWindow
   * 
   * <p>This class is strictly TreeSet related. equals method does not assure correct
   * comparison
   * 
   * @author snakePolygon.baniukiewicz
   *
   */
  class ExPolygon extends Polygon {
    private static final long serialVersionUID = 5870934217878285135L;
    public Rectangle initbounds; // initial size of polygon, before scaling
    public double scale; // current scale

    /**
     * Construct polygon from list of points.
     * 
     * @param data List of points
     */
    public ExPolygon(List<? extends Tuple2d> data) {
      // convert to polygon
      for (Tuple2d v : data) {
        addPoint((int) Math.round(v.getX()), (int) Math.round(v.getY()));
      }
      initbounds = new Rectangle(getBounds()); // remember original size
      scale = 1;
    }

    /**
     * Scale polygon to fit in rectangular window of size. Method changes internal polygon
     * representation. Fitting is done basing on bounding box area.
     * 
     * @param size Size of window to fit polygon
     */
    public void fitPolygon(double size) {
      // set in 0,0
      translate((int) Math.round(-initbounds.getCenterX()),
              (int) Math.round(-initbounds.getCenterY()));
      // get size of bounding box
      Rectangle2D bounds = getBounds2D();
      // set scale according to window size
      if (bounds.getWidth() > bounds.getHeight()) {
        scale = bounds.getWidth();
      } else {
        scale = bounds.getHeight();
      }
      scale = size / scale;
      scale *= 0.95; // little smaller than window
      for (int i = 0; i < npoints; i++) {
        xpoints[i] = (int) Math.round(xpoints[i] * scale);
        ypoints[i] = (int) Math.round(ypoints[i] * scale);
      }
      // center in window
      LOGGER.debug("Scale is: " + scale + " BoundsCenters: " + bounds.getCenterX() + " "
              + bounds.getCenterY());
      translate((int) Math.round(bounds.getCenterX()) + (int) (size / 2),
              (int) Math.round(bounds.getCenterY()) + (int) (size / 2));
    }

    /**
     * Scale polygon to fit in rectangular window of size using pre-computed bounding box and scale
     * 
     * <p>Use for setting next polygon on base of previous, when next has different shape
     * but must be centered with previous one.
     * 
     * @param size Size of window to fit polygon
     * @param init Bounding box to fit new polygon
     * @param scale Scale of new polygon
     */
    public void fitPolygon(double size, Rectangle2D init, double scale) {
      // set in 0,0
      this.scale = scale;
      LOGGER.debug("fitPolygon: Scale is: " + scale + " BoundsCenters: " + init.getCenterX() + " "
              + init.getCenterY());
      translate((int) Math.round(-init.getCenterX()), (int) Math.round(-init.getCenterY()));

      for (int i = 0; i < npoints; i++) {
        xpoints[i] = (int) Math.round(xpoints[i] * scale);
        ypoints[i] = (int) Math.round(ypoints[i] * scale);
      }
      translate((int) (size / 2), (int) (size / 2));
    }
  }

  /**
   * Class holding lower and upper index of window. Supports comparisons.
   * 
   * <p>Two ranges [lower;upper] and [l1;u1] are equal if any of these conditions is met:
   * <ol>
   * <li>they overlap
   * <li>they are the same
   * <li>one is included in second
   * </ol>
   * 
   * @author snakePolygon.baniukiewicz
   * @see WindowIndRange#compareTo(Object)
   */
  class WindowIndRange implements Comparable<Object> {
    public int lower;
    public int upper;

    public WindowIndRange() {
      upper = 0;
      lower = 0;
    }

    /**
     * Create pair of indexes that define window.
     * 
     * @param lower lower index
     * @param upper upper index
     */
    WindowIndRange(int l, int u) {
      setRange(l, u);
    }

    @Override
    public String toString() {
      return "{" + lower + "," + upper + "}";
    }

    public int hashCode() {
      int result = 1;
      result = 31 * result + lower;
      result = 31 * result + upper;
      return result;
    }

    /**
     * Compare two WindowIndRange objects.
     * 
     * @param obj object to compare
     * @return true only if ranges does not overlap
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }

      final WindowIndRange other = (WindowIndRange) obj;
      if (upper < other.lower) {
        return true;
      } else if (lower > other.upper) {
        return true;
      } else {
        return false;
      }

    }

    /**
     * Compare two WindowIndRange objects.
     * 
     * <p>The following rules of comparison are used:
     * <ol>
     * <li>If range1 is below range2 they are not equal
     * <li>If range1 is above range2 they are not equal
     * </ol>
     * 
     * <p>They are equal in all other cases:
     * <ol>
     * <li>They are sticked
     * <li>One includes other
     * <li>They overlap
     * </ol>
     * 
     * @param obj Object to compare to this
     * @return -1,0,1 expressing relations in windows positions
     */
    @Override
    public int compareTo(Object obj) {
      final WindowIndRange other = (WindowIndRange) obj;
      if (this == obj) {
        return 0;
      }

      if (upper < other.lower) {
        return -1;
      } else if (lower > other.upper) {
        return 1;
      } else {
        return 0;
      }
    }

    /**
     * Sets upper and lower indexes to the same value.
     * 
     * @param i Value to set for upper and lower
     */
    public void setSame(int i) {
      lower = i;
      upper = i;
    }

    /**
     * Set pair of indexes that define window assuring that lower is smaller than upper.
     * 
     * @param lower lower index, always smaller
     * @param upper upper index
     */
    public void setRange(int l, int u) {
      if (l > u) {
        this.lower = u;
        this.upper = l;
      } else {
        this.lower = l;
        this.upper = u;
      }
    }

  }

}
