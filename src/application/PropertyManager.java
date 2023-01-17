package application;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyManager extends Properties
{
  private static final long serialVersionUID = 1L;
  public static PropertyManager thePropertyManager = new PropertyManager();
  private static final String PATH = new String("res/configuration.properties");

  public static void load() throws IOException
  {
    // try to load it as if we are packaged into a jarfile
    try (InputStream resourceInput = PropertyManager.class.getClassLoader().getResourceAsStream(PATH))
    {
      // check to see if we have an inputstream
      if (resourceInput == null)
      {
        // load it from the file system
        try (InputStream fileIinput = new FileInputStream(PATH))
        {
          thePropertyManager.load(fileIinput);
        } catch (IOException ex)
        {
          throw ex;
        }
      } else
        thePropertyManager.load(resourceInput);

    } catch (IOException ex)
    {
      throw ex;
    }
  }

  public static void main(String[] args) throws IOException
  {
    load();
    
    // get the property value and print it out
    for (Object key : thePropertyManager.keySet())
    {
      System.out.println(key.toString() + " = " + thePropertyManager.get(key).toString());
    }

  }// public static void main(String[] args)

}// public class PropertyManager extends Properties
