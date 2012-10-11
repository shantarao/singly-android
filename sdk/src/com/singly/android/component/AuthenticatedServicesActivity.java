package com.singly.android.component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.singly.android.client.AsyncApiResponseHandler;
import com.singly.android.client.SinglyClient;
import com.singly.android.sdk.R;
import com.singly.android.util.JSONUtils;
import com.singly.android.util.SinglyUtils;

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
 * remove authentication for the service.  If accepted the Singly profile for 
 * the user for that service is deleted.
 * 
 * Different values can be passed in as intent extra information to change the
 * behavior of the AuthenticatedServicesActivity component.  They are:
 * 
 * <ol>
 *   <li>scopes - A bundle of service name to oauth scope parameter.</li>
 *   <li>flags - A bundle of service name to oauth flag parameter.</li>
 *   <li>includes - An array of the service names to include.  Only those
 *   services specified will be displayed.  If not set all services Singly 
 *   provides authentication for are displayed.</li>
 * </ol>   
 */
public class AuthenticatedServicesActivity
  extends Activity {

  private Context context;
  private SinglyClient singlyClient;
  private SinglyServicesAdapter servicesAdapter;
  private AsyncHttpClient httpClient = new AsyncHttpClient();

  // list of populated Singly services
  private List<SinglyService> services = new ArrayList<SinglyService>();
  private Set<String> includedServices = new HashSet<String>();

  // Map of service to user id on the service, set of authenticated services
  private Map<String, String> serviceIds = new HashMap<String, String>();
  private Set<String> authServices = new HashSet<String>();

  /**
   * Holds information about a Singly service from the /services endpoint.
   */
  private static class SinglyService {
    String id;
    String name;
    Map<String, String> icons;
    Bitmap iconBitmap;
  }

  /**
   * ViewHolder for optimizing the ListView.
   */
  private static class ServiceViewHolder {
    ImageView icon;
    TextView name;
    CheckBox authenticated;
  }

  /**
   * ArrayAdapter class that set the checkbox to checked if the user is 
   * authenticated against the service.
   */
  private class SinglyServicesAdapter
    extends ArrayAdapter<SinglyService> {

    private final Context context;
    private final List<SinglyService> services;
    private final Set<String> authServices;

    public SinglyServicesAdapter(Context context, List<SinglyService> services,
      Set<String> authServices) {

      super(context, R.layout.authenticated_services_row, services);
      this.context = context;
      this.services = services;
      this.authServices = authServices;
    }

    @Override
    public View getView(int position, View serviceView, ViewGroup parent) {

      final SinglyService service = services.get(position);
      ServiceViewHolder viewHolder = null;

      // view holder pattern to optimize loading of the ListView, the inflater
      // only runs once per row
      if (serviceView == null) {

        LayoutInflater inflater = (LayoutInflater)context
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        serviceView = inflater.inflate(R.layout.authenticated_services_row,
          parent, false);

        TextView textView = (TextView)serviceView.findViewById(R.id.textView1);
        CheckBox checkBox = (CheckBox)serviceView.findViewById(R.id.checkBox1);
        ImageView imageView = (ImageView)serviceView
          .findViewById(R.id.iconView1);

        viewHolder = new ServiceViewHolder();
        viewHolder.icon = imageView;
        viewHolder.name = textView;
        viewHolder.authenticated = checkBox;

        serviceView.setTag(viewHolder);
      }
      else {
        viewHolder = (ServiceViewHolder)serviceView.getTag();
      }

      // update the icon, name, and checkbox. This is important to do as the
      // row Views are reused
      viewHolder.icon.setImageBitmap(service.iconBitmap);
      viewHolder.name.setText(service.name);
      viewHolder.authenticated.setChecked(authServices.contains(service.id));

      return serviceView;
    }
  }

  /**
   * Gets the services that the user is authenticated for and then updates the
   * ListView checkboxes for those services.
   */
  private void updateAuthenticatedServices() {

    // don't get the authenticated services unless we have an access token
    if (!singlyClient.isAuthenticated(this)) {
      return;
    }

    // get the access token and pass it in as a query parameter
    Map<String, String> qparams = new LinkedHashMap<String, String>();
    String accessToken = SinglyUtils.getAccessToken(context);
    if (accessToken != null) {
      qparams.put("access_token", accessToken);
    }

    // get all the services the user is authenticated against
    singlyClient.doGetApiRequest(this, "/profiles", qparams,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // get the set of services from the response and populate the service
          // to user id mapping
          JsonNode root = JSONUtils.parse(response);
          List<String> profileNames = JSONUtils.getFieldnames(root);
          for (String profileName : profileNames) {
            if (!profileName.equals("id")) {
              authServices.add(profileName);
              List<String> profileIds = JSONUtils.getStrings(root, profileName);
              if (profileIds != null && !profileIds.isEmpty()) {
                serviceIds.put(profileName, profileIds.get(0));
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

  /**
   * Asynchronously downloads the icon image file for the Singly service and 
   * writes it out to local storage.
   * 
   * @param service The Singly service for which to download the icon.
   */
  private void downloadIcon(final SinglyService service) {

    // icon size is 32x32
    final String imageUrl = service.icons.get("32x32");
    if (imageUrl != null && service.iconBitmap == null) {

      // download and cache the icon in an async manner
      httpClient.get(imageUrl, null, new BinaryHttpResponseHandler() {

        @Override
        public void onSuccess(byte[] imageBytes) {

          if (imageBytes != null && imageBytes.length > 0) {

            try {

              // get the location in internal storage of the file
              Context appContext = context.getApplicationContext();
              File dataDir = appContext.getFilesDir();
              File singlyStorage = new File(dataDir, "singly");

              // write out the downloaded bytes to a file
              FileUtils.writeByteArrayToFile(new File(singlyStorage, service.id
                + ".img"), imageBytes);

              // use the bytes to create a Bitmap, then notify the ListView that
              // the data has changed, redraw
              service.iconBitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
                imageBytes.length);
              servicesAdapter.notifyDataSetChanged();
            }
            catch (IOException e) {
              // do nothing, no icon wrote to local storage, nothing updated
            }
          }
        }
      });
    }
  }

  /**
   * Retrieves a service icon file from local storage if it hasn't already been
   * retrieved for this application.
   * 
   * @param service The Singly service for which to retrieve the icon.
   */
  private void getIconFromLocalStorage(SinglyService service) {

    // only read from local storage if the icon hasn't already been decompressed
    // in this application
    if (service.iconBitmap == null) {
      try {

        // get the location in internal storage of the file
        Context appContext = context.getApplicationContext();
        File dataDir = appContext.getFilesDir();
        File singlyStorage = new File(dataDir, "singly");
        File serviceIcon = new File(singlyStorage, service.id + ".img");

        // if the file exists then read the file from internal storage and turn
        // it into an Bitmap
        if (serviceIcon.exists()) {
          byte[] imageBytes = FileUtils.readFileToByteArray(serviceIcon);
          service.iconBitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
            imageBytes.length);
        }
      }
      catch (IOException e) {
        // do nothing, no icon writter, nothing updated
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_authenticated_services);

    // set the context and any scopes specified
    this.context = this;
    Intent intent = getIntent();

    // scopes and flags parameters for each service
    final Bundle oauthScopes = intent.getBundleExtra("scopes");
    final Bundle oauthFlags = intent.getBundleExtra("flags");

    // setup a service inclusion list if needed
    String[] includesAr = intent.getStringArrayExtra("includes");
    if (includesAr != null && includesAr.length > 0) {
      includedServices.addAll(Arrays.asList(includesAr));
    }

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
        SinglyService service = services.get(pos);
        CheckBox checkBox = (CheckBox)item.findViewById(R.id.checkBox1);
        final boolean authenticated = checkBox.isChecked();
        final String serviceId = service.id;
        final String serviceName = service.name;

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
                String serviceUserId = serviceIds.get(serviceId);
                qparams.put("delete", serviceUserId + "@" + serviceId);

                // add the access token
                String accessToken = SinglyUtils.getAccessToken(context);
                if (accessToken != null) {
                  qparams.put("access_token", accessToken);
                }

                // this calls a profile delete, success means the user is no
                // longer authenticated with the service
                singlyClient.doPostApiRequest(context, "/profiles", qparams,
                  new AsyncApiResponseHandler() {

                    @Override
                    public void onSuccess(String response) {

                      // remove the service to id from the mapping
                      serviceIds.remove(serviceId);

                      // remove from authenticated services
                      authServices.remove(serviceId);

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

                // get oauth scope and flag
                Map<String, String> authExtra = new LinkedHashMap<String, String>();
                if (oauthScopes != null) {
                  authExtra.put("scope", oauthScopes.getString(serviceId));
                }
                if (oauthFlags != null) {
                  authExtra.put("flag", oauthFlags.getString(serviceId));
                }

                // not authenticated, follow the normal authentication process
                // via an AuthenticationActivity
                singlyClient.authenticate(context, serviceId, authExtra);
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
    servicesAdapter = new SinglyServicesAdapter(this, services, authServices);
    mainListView.setAdapter(servicesAdapter);
  }

  @Override
  protected void onStart() {

    super.onStart();

    // do a call to singly to get all the available services
    singlyClient.doGetApiRequest(this, "/services", null,
      new AsyncApiResponseHandler() {

        @Override
        public void onSuccess(String response) {

          // new list of services
          List<SinglyService> curServices = new ArrayList<SinglyService>();
          boolean onlyIncluded = !includedServices.isEmpty();

          JsonNode rootNode = JSONUtils.parse(response);
          Map<String, JsonNode> serviceNodes = JSONUtils.getFields(rootNode);

          // loop through the service name to objects
          for (Map.Entry<String, JsonNode> entry : serviceNodes.entrySet()) {

            // parse and add the service to the services list
            JsonNode serviceNode = entry.getValue();
            SinglyService singlyService = new SinglyService();
            singlyService.id = entry.getKey();
            singlyService.name = StringUtils.capitalize(JSONUtils.getString(
              serviceNode, "name"));

            // if we have an include set only use services in the set
            if (onlyIncluded && !includedServices.contains(singlyService.id)) {
              continue;
            }

            // create a map of the icons and their sizes
            Map<String, String> icons = new HashMap<String, String>();
            List<JsonNode> iconNodes = JSONUtils.getJsonNodes(serviceNode,
              "icons");
            for (JsonNode iconNode : iconNodes) {
              int height = JSONUtils.getInt(iconNode, "height");
              int width = JSONUtils.getInt(iconNode, "width");
              String source = JSONUtils.getString(iconNode, "source");
              String key = height + "x" + width;
              icons.put(key, source);
            }
            singlyService.icons = icons;

            // if possible retrieve a previously downloaded icon, if not then
            // download and store it in an async manner
            getIconFromLocalStorage(singlyService);
            if (singlyService.iconBitmap == null) {
              downloadIcon(singlyService);
            }

            curServices.add(singlyService);
          }

          // sort the services by name
          Collections.sort(curServices, new Comparator<SinglyService>() {

            @Override
            public int compare(SinglyService lhs, SinglyService rhs) {
              return lhs.name.compareTo(rhs.name);
            }
          });

          // clear and update the services list
          services.clear();
          services.addAll(curServices);

          // display the changes
          servicesAdapter.notifyDataSetChanged();
          updateAuthenticatedServices();
        }

        @Override
        public void onFailure(Throwable error) {

        }
      });
  }
}
