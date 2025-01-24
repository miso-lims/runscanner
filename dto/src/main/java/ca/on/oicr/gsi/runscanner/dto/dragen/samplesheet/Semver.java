package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

public class Semver implements Comparable<Semver> {

  private int major, minor, patch;

  public Semver(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  @Override
  public int compareTo(Semver other) {
    if (this.major > other.major) {
      return 1;
    } else if (this.major < other.major) {
      return -1;
    } else { // majors equal
      if (this.minor > other.minor) {
        return 1;
      } else if (this.minor < other.minor) {
        return -1;
      } else { // major and minor equal
        return Integer.compare(this.patch, other.patch);
      }
    }
  }
}
