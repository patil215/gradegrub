package com.manateams.android.manateams.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.manateams.android.manateams.AssignmentActivity;
import com.manateams.android.manateams.R;
import com.manateams.android.manateams.asynctask.AssignmentLoadTask;
import com.manateams.android.manateams.asynctask.AsyncTaskCompleteListener;
import com.manateams.android.manateams.asynctask.CourseLoadTask;
import com.manateams.android.manateams.util.Constants;
import com.manateams.android.manateams.util.DataManager;
import com.manateams.android.manateams.views.CourseAdapter;
import com.manateams.android.manateams.views.RecyclerItemClickListener;
import com.quickhac.common.data.ClassGrades;
import com.quickhac.common.data.Course;

import org.ocpsoft.prettytime.PrettyTime;
import org.w3c.dom.Text;

import java.util.Date;

public class CourseFragment extends Fragment implements AsyncTaskCompleteListener, SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView coursesList;
    private TextView lastUpdatedText;

    private Course[] courses;
    private DataManager dataManager;
    private CourseAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_courses, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) /*called after layout created */{
        super.onActivityCreated(savedInstanceState);
        dataManager = new DataManager(getActivity());
        courses = dataManager.getCourseGrades();
        setupViews();
    }

    private void setupViews() {
        lastUpdatedText = (TextView) getActivity().findViewById(R.id.text_lastupdated);
        // Set relative time for last updated
        PrettyTime p = new PrettyTime();
        lastUpdatedText.setText("Last updated " + p.format(new Date(dataManager.getOverallGradesLastUpdated())) + " - pull down to refresh");

        coursesList = (RecyclerView) getActivity().findViewById(R.id.list_courses);
        coursesList.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        coursesList.setItemAnimator(new DefaultItemAnimator());

        // Set the grade cards
        adapter = new CourseAdapter(getActivity(), courses);
        coursesList.setAdapter(adapter);
        coursesList.addOnItemTouchListener(
                new RecyclerItemClickListener(this.getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if(dataManager.getClassGrades(position) != null) {
                            startAssignmentActivity(position);
                        } else {
                            loadAssignmentsForCourse(position);
                        }
                    }
                })
        );
        swipeRefreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(R.color.app_primary, R.color.app_accent);
        swipeRefreshLayout.setEnabled(true);
    }

    public void restartActivity() {
        Intent intent = getActivity().getIntent();
        getActivity().overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        getActivity().finish();

        getActivity().overridePendingTransition(0, 0);
        startActivity(intent);
    }

    public void loadAssignmentsForCourse(int position) {
        new AssignmentLoadTask(this, getActivity()).execute(new String[] {dataManager.getUsername(), dataManager.getPassword(), dataManager.getStudentId(), String.valueOf(position)});
    }

    @Override
    public void onCoursesLoaded(Course[] courses) {
        if(courses != null) {
            dataManager.setCourseGrades(courses);
            dataManager.setOverallGradesLastUpdated();
            //Todo Detection of current cycle
            for (Course c: courses){
                dataManager.addCourseDatapoint(c.semesters[0].average,c.courseId);
            }
            restartActivity();
        }
    }

    @Override
    public void onClassGradesLoaded(ClassGrades[] grades, int courseIndex) {
        dataManager.setClassGrades(grades, courseIndex);
        dataManager.setCourseGradesLastUpdated(courses[courseIndex].courseId);
        startAssignmentActivity(courseIndex);
    }

    public void startAssignmentActivity(int courseIndex) {
        Intent intent = new Intent(getActivity(), AssignmentActivity.class);
        intent.putExtra(Constants.EXTRA_COURSEINDEX, courseIndex);
        intent.putExtra(Constants.EXTRA_COURSETITLE, courses[courseIndex].title);
        intent.putExtra(Constants.EXTRA_COURSEID, courses[courseIndex].courseId);
        startActivity(intent);

    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 5000);
        new CourseLoadTask(this, getActivity()).execute(dataManager.getUsername(), dataManager.getPassword(), dataManager.getStudentId());
    }
}
