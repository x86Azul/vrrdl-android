package edu.depaul.x86azul;

/**
 * Static class for global parameters.
 */
public class GP {
  private GP() {}
  
  public static enum AppState {CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED};

  /*
   * state for the main activity
   */
  public static volatile AppState state = AppState.CREATED;
  
  /*
   * whether tap to map means insert debris
   */
  public static boolean tapMeansInsert = true;
  
  /*
   * flag to decide what action should/shouldn't be performed
   * when under test (e.g. sycn test data to webservice)
   */
  public static boolean testMode = false;
  
  /*
   * the radius where alert will get triggered detect debris within 
   * this circle
   */
  public static double dangerRadiusInMeter = 2414.02; // 1.5 miles

  
  /*
   * the address of vrrdl web service
   */
  public static String webServiceURI = "http://vrrdl.elasticbeanstalk.com";
  
  public static boolean isVisibleState(){
	  return (state == AppState.STARTED || 
			  state == AppState.RESUMED || 
			  state == AppState.PAUSED);
  }

}  
