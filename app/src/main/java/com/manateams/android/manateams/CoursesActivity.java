package com.manateams.android.manateams;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.manateams.android.manateams.fragments.AboutFragment;
import com.manateams.android.manateams.fragments.CourseFragment;
import com.manateams.android.manateams.fragments.GPAFragment;
import com.manateams.android.manateams.fragments.SettingsFragment;
import com.manateams.android.manateams.util.DataManager;
import com.manateams.android.manateams.util.Utils;
import com.manateams.android.manateams.views.DrawerAdapter;
import com.manateams.scraper.data.Course;

import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

import static android.content.DialogInterface.BUTTON_POSITIVE;


public class CoursesActivity extends ActionBarActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private Handler mHandler = new Handler();
    private String mTitle;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);
        setupDrawer();
        selectItem(0);
        dataManager = new DataManager(this);
        //TODO Fine tune these values, see https://github.com/hotchemi/Android-Rate
        AppRate.with(this)
                .setInstallDays(1)
                .setLaunchTimes(3)
                .setRemindInterval(3)
                .setOnClickButtonListener(new OnClickButtonListener() {
                    @Override
                    public void onClickButton(int which) {
                        if (which == BUTTON_POSITIVE) {
                            sendShareIntent();
                        }
                    }
                })
                .monitor();
        
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.updateSecurityProvider(this);
        //Utils.showConnectedtoAISDGuestDialog(this, this);
    }

    private void setupDrawer() {
        //Define Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String[] mDrawerArray = getResources().getStringArray(R.array.drawer_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerAdapter(this,
                R.layout.navdrawer_item, mDrawerArray));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                CourseFragment courseFragment = new CourseFragment();
                //TODO Possible animation here?
                fragmentManager.beginTransaction().replace(R.id.content_frame, courseFragment).commit();
                break;
            case 1:
                GPAFragment gpaFragment = new GPAFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, gpaFragment).commit();
                break;
            case 2:
                SettingsFragment fragment = new SettingsFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, (Fragment) fragment).commit();
                break;
            case 3:
                AboutFragment aboutFragment = new AboutFragment();
                fragmentManager.beginTransaction().replace(R.id.content_frame, aboutFragment).commit();
                break;
        }
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mTitle = getResources().getStringArray(R.array.drawer_array)[position];
        getSupportActionBar().setTitle(mTitle);
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }, 150);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.courses, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_logout:
                dataManager.setCredentials(null, null, null,null,null);
                Course[] courses = dataManager.getCourseGrades();
                for(int i = 0; i < courses.length; i++) {
                    dataManager.setClassGrades(null, courses[i].courseId);
                    dataManager.deleteDatapoints(courses[i].courseId);
                }
                dataManager.setCourseGrades(null);
                dataManager.invalidateCookie();
                dataManager.setUserIdentification(null);
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.action_share:
                sendShareIntent();
                break;
        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void sendShareIntent() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check your grades with manaTEAMS http://manateams.com");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    public void onNeilWebsiteClick(View v) {
        String url = "http://patil215.github.io";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void onNeilEmailClick(View v) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "neilpatil215@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    public void onEhsanWebsiteClick(View v) {
        String url = "http://ehsandev.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
    public void onEhsanEmailClick(View v) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "ehsan@ehsandev.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }
}
