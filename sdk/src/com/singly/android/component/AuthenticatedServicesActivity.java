package com.singly.android.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.sdk.R;
import com.singly.android.util.JSON;

/**
 * An Activity that shows which Singly service a user is authenticated against
 * in a ListView.
 * 
 * Each row in the ListView contains the name of the service and a checkbox that
 * is checked if the user is authenticated against that service.  
 * 
 * Clicking an unchecked row will open a dialog that allows the user to choose
 * to authenticate against that service.  If accepted then the user follow the
 * normal authentication process via the authenticate method of the Singly 
 * client which launches an AuthenticationActivity. 
 * 
 * Clicking a checked row will open a dialog that allows the user to choose to
 * deauthenticate from the service.  If accepted the Singly profile for that 
 * user for that service is deleted.
 */
public class AuthenticatedServicesActivity
  extends Activity {

  private SinglyClient singlyClient;
  private SinglyServicesAdapter servicesAdapter;

  // services supported by Singly
  private String[] services = new String[] {
    "bodymedia", "dropbox", "facebook", "fitbit", "flickr", "foursquare",
    "gcal", "gcontacts", "gdocs", "github", "gmail", "google", "gplus",
    "instagram", "klout", "linkedin", "meetup", "runkeeper", "stocktwits",
    "tout", "tumblr", "twitter", "withings", "wordpress", "yammer", "youtube",
    "zeo"
  };

  // Display names for the services
  private String[] serviceNames = new String[] {
    "BodyMedia", "Dropbox", "Facebook", "Fitbit", "Flickr", "Foursquare",
    "Google Calendar", "Google Contacts", "Google Docs", "Github", "Gmail",
    "Google", "Google Plus", "Instagram", "Klout", "LinkedIn", "Meetup",
    "RunKeeper", "StockTwits", "Tout", "Tumblr", "Twitter", "Withings",
    "WordPres", "Yammer", "YouTube", "Zeo"
  };

  // Map of service to user id on the service, set of authenticated services
  private Map<String, String> serviceIds = new HashMap<String, String>();
  private Set<String> authServices = new HashSet<String>();

  /**
   * ArrayAdapter class that set the checkbox to checked if the user is 
   * authenticated against the service.
   */
  private class SinglyServicesAdapter
    extends ArrayAdapter<String> {

    private final Context context;
    private final String[] services;
    private final String[] serviceNames;
    private final Set<String> authServices;

    public SinglyServicesAdapter(Context context, String[] services,
      String[] serviceNames, Set<String> authServices) {
      super(context, R.layout.authenticated_services_row, services);
      this.context = context;
      this.services = services;
      this.serviceNames = serviceNames;
      this.authServices = authServices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      LayoutInflater inflater = (LayoutInflater)context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View rowView = inflater.inflate(R.layout.authenticated_services_row,
        parent, false);

      TextView textView = (TextView)rowView.findViewById(R.id.textView1);
      CheckBox checkBox = (CheckBox)rowView.findViewById(R.id.checkBox1);

      textView.setText(serviceNames[position]);
      checkBox.setChecked(authServices.contains(services[position]));

      return rowView;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_authenticated_services);

    // get an instance of the singly client
    singlyClient = SinglyClient.getInstance();

    // get the main list view and put a click listener on it. This will tell
    // us which row was clicked, on the layout xml the checkbox is not focusable
    // or clickable directly, the row handles that
    ListView mainListView = (ListView)findViewById(R.id.authenticatedServicesList);
    mainListView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View item, int pos, long id) {

        // get the service and service name for the clicked row
        CheckBox checkBox = (CheckBox)item.findViewById(R.id.checkBox1);
        final boolean authenticated = checkBox.isChecked();
        final String service = services[pos];
        final String serviceName = serviceNames[pos];

        // create the alert ok/cancel dialog
        AlertDialog.Builder okCancelDialog = new AlertDialog.Builder(
          AuthenticatedServicesActivity.this);

        // if authenticated pop dialog to de-authenticate, else to authenticate
        if (authenticated) {
          okCancelDialog.setTitle("Remove " + serviceName);
          okCancelDialog.setMessage("Remove authentication for " + serviceName);
        }
        else {
          okCancelDialog.setTitle("Add " + serviceName);
          okCancelDialog.setMessage("Authenticate with " + serviceName);
        }

        // they clicked the ok button
        okCancelDialog.setPositiveButton("OK",
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dint, int which) {

              // if currently authenticated then we are de-authenticating, do
              // a POST request to delete the profile from the user's Singly
              // account
              if (authenticated) {

                Map<String, String> qparams = new HashMap<String, String>();
                String serviceId = serviceIds.get(service);
                qparams.put("delete", serviceId + "@" + service);

                // success means the user is no longer authenticated in Singly
                // for that service
                singlyClient.doPostApiRequest(
                  AuthenticatedServicesActivity.this, "/profiles", qparams,
                  new AsyncApiResponseHandler() {

                    @Override
                    public void onSuccess(String response) {

                      // remove the service to id from the mapping
                      serviceIds.remove(service);
                      
                      // remove from authenticated services
                      authServices.remove(service);

                      // update the list view
                      servicesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                      // nothing on failure, maybe we should show a dialog
                    }
                  });

              }
              else {

                // not authenticated, follow the normal authentication process
                // via an AuthenticationActivity
                singlyClient.authenticate(AuthenticatedServicesActivity.this,
                  service);
              }
            }
          });

        // they clicked the cancel button
        okCancelDialog.setNegativeButton("Cancel", null);

        // show the dialog
        okCancelDialog.show();
      }
    });

    // set the services array adapter into the main list view
    servicesAdapter = new SinglyServicesAdapter(this, services, serviceNames,
      authServices);
    mainListView.setAdapter(servicesAdapter);
  }

  @Override
  protected void onStart() {
    super.onStart();

    // don't get the authenticated services unless we have an access token
    if (!singlyClient.isAuthenticated(this)) {
      return;
    }

    // get all the services the user is authenticated against
    singlyClient.doGetApiRequest(this, "/profiles", null,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // get the set of services from the response and populate the service
          // to user id mapping
          JSONObject jsonObj = JSON.parse(response);
          for (Iterator iter = jsonObj.keys(); iter.hasNext();) {
            String profile = (String)iter.next();
            if (!profile.equals("id")) {
              authServices.add(profile);
              List<String> profileIds = JSON.getStrings(jsonObj, profile);
              if (profileIds != null && !profileIds.isEmpty()) {
                serviceIds.put(profile, profileIds.get(0));
              }
            }
          }

          // notify the list view that the data has changed, update view
          servicesAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFailure(Throwable error) {

        }
      });
  }

}
