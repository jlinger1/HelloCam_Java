package application;

public enum OV5642
{  
  OV5642_320x240(0, "320x240"),
  OV5642_640x480(1, "640x480"),
  OV5642_1024x768(2, "1024x768"),
  OV5642_1280x960(3, "1280x960"),
  OV5642_1600x1200(4, "1600x1200"),
  OV5642_2048x1536(5, "2048x1536"),
  OV5642_2592x1944(6, "2592x1944");
  
  private final int mode;
  private final String name;
  
  OV5642(int mode, String name)
  {
    this.mode = mode;
    this.name = name;
  }
  
  public int getMode()
  {
    return mode;
  }
  
  public String getName()
  {
    return name;
  }
  
  public String toString()
  {
    return name;
  }
  
  public static OV5642 valueOf(int mode)
  {
    for(OV5642 m : OV5642.values())
      if(mode == m.getMode())
        return m;
    return null;
  }  
}
