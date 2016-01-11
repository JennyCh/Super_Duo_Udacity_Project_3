package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.api.Callback;


public class MainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, Callback, BookDetail.BookDetailCallback {

    public static final String LOG_TAG = "MainActivity";
    Fragment nextFragment;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence title;
    public static boolean IS_TABLET = false;
    private BroadcastReceiver messageReciever;

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";
    BookDetail fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
     //   Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
       /* IS_TABLET = isTablet();
        if(IS_TABLET){
            setContentView(R.layout.activity_main_sw600dp);
        }else {
            setContentView(R.layout.activity_main);
        }
*/
        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever,filter);
        setContentView(R.layout.activity_main);
      //  Log.v(LOG_TAG, "IS_TABLET onCreate" + String.valueOf(IS_TABLET));
        if(findViewById(R.id.right_container) != null){
            IS_TABLET = true;
        }
     //   Log.v(LOG_TAG, "IS_TABLET postCreate" + String.valueOf(IS_TABLET));

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        title = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
       // Log.v(LOG_TAG, "onActivityResult");
        IntentResult scanningResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            if("EAN_13".equals(scanFormat)){
                TextView isbn = (TextView) findViewById(R.id.ean);
                isbn.setText(scanContent);
            }
            //Log.v("AddBook ", scanContent + " " + scanFormat);
            //Toast toast = Toast.makeText(this, scanContent + "  " + scanFormat, Toast.LENGTH_SHORT);
            //toast.show();
        } else {
            Toast toast = Toast.makeText(this, "No scan data received", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
     //   Log.v(LOG_TAG, "onNavigationDrawerItemSelected");
        FragmentManager fragmentManager = getSupportFragmentManager();
        Bundle bundle = new Bundle();

        //deleted right fragment when not on List of Books, because its not needed
        Fragment f = fragmentManager.findFragmentById(R.id.right_container);

        switch (position){
            default:
            case 0:
                this.nextFragment = new ListOfBooks();
                break;
            case 1:
                this.nextFragment = new AddBook();
                if (f != null){
                  //  Log.v(LOG_TAG, "FOUND FRAGMENT");
                    fragmentManager.beginTransaction().remove(f).commit();
                }
                break;
            case 2:
                this.nextFragment = new About();
                if (f != null){
                   // Log.v(LOG_TAG, "FOUND FRAGMENT");
                    fragmentManager.beginTransaction().remove(f).commit();
                }
                break;

        }

        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                //.addToBackStack((String) title)
                .commit();

        // supposed to hide right_fragment, so it does not display random things with unrelated screens
       // fragmentManager.beginTransaction().hide(R.id.right_container).commit();
    }
    public void bookDeleted(){
        // MADE LIST OF BOOKS INSTANTLY REFRESH WHEN BOOK IS DELETED
        this.nextFragment = new ListOfBooks();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, nextFragment).commit();
        //Log.v(LOG_TAG, "REFRESHED LIST");
    }

    public void setTitle(int titleId) {
        title = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // Log.v(LOG_TAG, "onCreateOptionsMenu");
        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);

            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
       // Log.v(LOG_TAG, "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

    @Override
    public void onItemSelected(String ean) {
      //  Log.v(LOG_TAG, "Item recieved " + ean);
        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);

        fragment = new BookDetail();
        fragment.setArguments(args);
      //  Log.v(LOG_TAG, "IS_TABLET " + String.valueOf(IS_TABLET));
      //  Log.v(LOG_TAG, "container " +  String.valueOf(findViewById(R.id.right_container)));
        if(IS_TABLET && (findViewById(R.id.right_container) != null)){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.right_container, fragment,"DETAIL_BOOK").commit();
           // Log.v(LOG_TAG, "Right container is not null " + R.id.right_container);
                            //.addToBackStack("Book Detail")
        }else{
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment).commit();
          //  Log.v(LOG_TAG, "Right container is null");
        }


    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
           // Log.v(LOG_TAG, "onReceive");
            if(intent.getStringExtra(MESSAGE_KEY)!=null){
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

  /*  public void goBack(View view){
        Log.v(LOG_TAG, "goBack");
        getSupportFragmentManager().popBackStack();
    }*/

    private boolean isTablet() {
      //  Log.v(LOG_TAG, "isTablet");
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

/*    @Override
    public void onBackPressed() {
        Log.v(LOG_TAG, "onBackPressed");
        if(getSupportFragmentManager().getBackStackEntryCount()<2){
            finish();
        }
        super.onBackPressed();
    }*/


}