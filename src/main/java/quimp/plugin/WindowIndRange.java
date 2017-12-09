package quimp.plugin;

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
  WindowIndRange(int lower, int upper) {
    setRange(lower, upper);
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
