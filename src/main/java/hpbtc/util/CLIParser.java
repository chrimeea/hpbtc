package hpbtc.util;

public class CLIParser {

  private String[] args;

  public CLIParser(String[] args) {
    this.args = args;
  }

  public boolean hasArg(String prefix) {
    return this.getArgIndex(prefix) >= 0;
  }

  private int getArgIndex(String prefix) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(prefix)) {
        return i;
      }
    }
    return -1;
  }

  public String getArgValue(String prefix) {
    int i = this.getArgIndex(prefix);
    if (i >= 0 && i < args.length - 1) {
      return args[i + 1];
    } else {
      return null;
    }
  }

}
