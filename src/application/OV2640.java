package application;

public enum OV2640
{  
  OV2640_160x120(0, "160x120"),
  OV2640_176x144(1, "176x144"),
  OV2640_320x240(2, "320x240"),
  OV2640_352x288(3, "352x288"),
  OV2640_640x480(4, "640x480"),
  OV2640_800x600(5, "800x600"),
  OV2640_1024x768(6, "1024x768"),
  OV2640_1280x1024(7, "1280x1024"),
  OV2640_1600x1200(8, "1600x1200");
  
  private final int mode;
  private final String name;
  
  OV2640(int mode, String name)
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
  
  public static OV2640 valueOf(int mode)
  {
    for(OV2640 m : OV2640.values())
      if(mode == m.getMode())
        return m;
    return null;
  }  
}
