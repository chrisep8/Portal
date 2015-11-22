package com.directdev.portal.tools.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.directdev.portal.R;
import com.directdev.portal.tools.fetcher.FetchAccountData;
import com.directdev.portal.tools.helper.Pref;
import com.directdev.portal.tools.model.Course;
import com.directdev.portal.tools.model.Dates;
import com.directdev.portal.tools.model.Exam;
import com.directdev.portal.tools.model.Finance;
import com.directdev.portal.tools.model.Grades;
import com.directdev.portal.tools.model.GradesCourse;
import com.directdev.portal.tools.model.Schedule;
import com.directdev.portal.tools.model.Terms;
import com.directdev.portal.tools.event.UpdateFailedEvent;
import com.directdev.portal.tools.event.UpdateFinishEvent;
import com.directdev.portal.tools.helper.Request;
import com.directdev.portal.tools.helper.VolleySingleton;
import com.directdev.portal.tools.helper.GsonHelper;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import io.realm.RealmResults;

/**  All data and api request to the server are handled by this service, to update all data we can call
 *  UpdateService.all() anywhere in the application. To update one data only, we can call the specific
 *  function for that data (eg. UpdateService.schedule()).
 *
 *  How this work:
 *  1. We call either the all() function or specific functions(schedule(),exam()...). These are
 *     helper functions.
 *
 *  2. The helper function that is called will prepare the intent and set the action(SCHEDULE, EXAM)
 *     and then call startService() which will launch the service. Service is a singleton, only one
 *     instance of this service will be created, if service already exist and startService is called
 *     the startService() call will be queued. When the service is launched, static field isActive will
 *     be set to true.
 *
 *  3. startService() will start this service and the trigger onHandleIntent(). onHandleIntent() will
 *     then look and the action that is set on step 2, check which on it's and launch the appropriate
 *     function(handleSchedule(), handleExam())
 *
 *  4. The handle functions(handleExam(), handleSchedule()) will request the data and returns it using
 *     future, the data then will be saved into a realm database, if the data failed to be saved(because
 *     session expired and other things were sent to us), using EventBus, we send an UpdateFailedEvent
 *    which then will be captured by our activity to display the please refresh message.
 *
 *  5. When onHandleIntent() finished (choosing which handle function to run, run it, receive data,
 *     save data to Realm DB), the service will be closed and onDestroy() is called, but if multiple
 *     startService() was called, onHandleIntent() will be called again to serve the queued startService()
 *     calls until all call is served, and then onDestroy() is called. onDestroy will call EventBus and
 *     sent and UpdateFinishEvent, which will then be used to refresh data on the views. Also isActive
 *     will be set to false.
 */

public class UpdateService extends IntentService {
    private static final String SCHEDULE = "com.directdev.portal.tools.services.action.SCHEDULE";
    private static final String EXAM = "com.directdev.portal.tools.services.action.EXAM";
    private static final String FINANCE = "com.directdev.portal.tools.services.action.FINANCE";
    private static final String GRADES = "com.directdev.portal.tools.services.action.GRADES";
    private static final String TERMS = "com.directdev.portal.tools.services.action.TERMS";
    private static final String COURSE = "com.directdev.portal.tools.services.action.COURSE";
    private static final String ACCOUNT = "com.directdev.portal.tools.services.action.ACCOUNT";
    public static boolean isActive = false;

    public UpdateService() {
        super("UpdateService");
    }

    // Below are helper methods to prepare intents to start this UpdateService service.
    public static void all(Context ctx){
        if(!isActive) {
            UpdateService.terms(ctx);
            UpdateService.exam(ctx);
            UpdateService.schedule(ctx);
            UpdateService.finance(ctx);
            UpdateService.grades(ctx);
            UpdateService.course(ctx);
            UpdateService.account(ctx);
        }
    }

    public static void schedule(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(SCHEDULE);
        ctx.startService(intent);
    }

    public static void exam(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(EXAM);
        ctx.startService(intent);
    }

    public static void finance(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(FINANCE);
        ctx.startService(intent);
    }

    public static void terms(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(TERMS);
        ctx.startService(intent);
    }

    public static void grades(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(GRADES);
        ctx.startService(intent);
    }

    public static void course(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(COURSE);
        ctx.startService(intent);
    }

    public static void account(Context ctx) {
        Intent intent = new Intent(ctx, UpdateService.class);
        intent.setAction(ACCOUNT);
        ctx.startService(intent);
    }

    @Override
    public void onCreate() {
        isActive = true;
        Log.d("onCreate", "Called !!!!!!");
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (SCHEDULE.equals(action)) {
                handleSchedule();
            } else if (EXAM.equals(action)) {
                handleExam();
            }else if (FINANCE.equals(action)) {
                handleFinance();
            }else if (GRADES.equals(action)) {
                handleGrades();
            }else if (TERMS.equals(action)) {
                handleTerms();
            }else if (COURSE.equals(action)) {
                handleCourse();
            }else if (ACCOUNT.equals(action)) {
                handleAccount();
            }
        }
    }
    //Everything below handles the data request and Manipulation
    private void handleSchedule() {

        //Creates a future, volley usually request data Asynchronously, we use future to make volley
        //do some sort of Synchronous request.
        RequestFuture<String> future = RequestFuture.newFuture();

        //We make the requestQueue.
        RequestQueue queue = VolleySingleton.getInstance(this).getQueue();

        //Add a new request to the RequestQueue
        queue.add(Request.create(this, getString(R.string.request_schedule), future, future));
        String response = "";
        try{
            //Get the response from future.
            response = future.get();
        } catch (Exception e) {}
        try{
            //Turns the response(Which is a JSONArray) to a list of object(Here is Schedule and Dates Objects)
            List<Schedule> schedules = GsonHelper.create().fromJson(response, new TypeToken<List<Schedule>>() {
            }.getType());
            List<Dates> dates = GsonHelper.create().fromJson(response, new TypeToken<List<Dates>>() {
            }.getType());

            //Save the objects that is return by Gson to realm
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.clear(Schedule.class);
            realm.copyToRealm(schedules);
            realm.copyToRealmOrUpdate(dates);
            realm.commitTransaction();
            realm.close();
        }catch (JsonSyntaxException e) {
            //Called when request fails
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }
    }

    private void handleExam() {
        RequestFuture<String> future = RequestFuture.newFuture();
        RequestQueue queue = VolleySingleton.getInstance(this).getQueue();
        queue.add(Request.create(this, getString(R.string.request_exam), future,future));
        String response = "";
        try{
            response = future.get();
        } catch (Exception e) {}

        try{
            List<Exam> exams = GsonHelper.create().fromJson(response, new TypeToken<List<Exam>>() {
            }.getType());
            List<Dates> dates = GsonHelper.create().fromJson(response, new TypeToken<List<Dates>>() {
            }.getType());
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.clear(Exam.class);
            realm.copyToRealm(exams);
            realm.copyToRealmOrUpdate(dates);
            realm.commitTransaction();
            realm.close();
        }catch (JsonSyntaxException e){
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }
    }

    private void handleFinance() {
        RequestFuture<String> future = RequestFuture.newFuture();
        RequestQueue queue = VolleySingleton.getInstance(this).getQueue();
        queue.add(Request.create(this, getString(R.string.request_finance),future, future));
        String data;
        String response = "";
        try{
            response = future.get();
        } catch (Exception e) {}
        try {

            /**
             * Finance data is structured like this {"Status":[*Data that we want*]}. The GSON requires
             * JSONArray to turn it into a List of object(List<Finance>). So we have to get the JSONArray
             * out of the "status" property of the JSONObject
             */
            JSONObject finance = new JSONObject(response);
            data = finance.getJSONArray("Status").toString();

            //Then we do the usual
            List<Finance> finances = GsonHelper.create().fromJson(data, new TypeToken<List<Finance>>() {
            }.getType());
            List<Dates> dates = GsonHelper.create().fromJson(data, new TypeToken<List<Dates>>() {
            }.getType());
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.clear(Finance.class);
            realm.copyToRealm(finances);
            realm.copyToRealmOrUpdate(dates);
            realm.commitTransaction();
            realm.close();
        } catch (JSONException e) {
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }
    }

    /**
     * Requesting grades is a bit more complicated, there are one link for each terms, to
     * build the links, we takes the prefix(request_grades string) and add to it the term names. That's
     * why this requires parameter while others doesn't
     *
     * Prefix:
     * https://newbinusmaya.binus.ac.id/services/ci/index.php/scoring/ViewGrade/getStudentScore/
     *
     * Final link that is called:
     * https://newbinusmaya.binus.ac.id/services/ci/index.php/scoring/ViewGrade/getStudentScore/1410
     * https://newbinusmaya.binus.ac.id/services/ci/index.php/scoring/ViewGrade/getStudentScore/1420
     * https://newbinusmaya.binus.ac.id/services/ci/index.php/scoring/ViewGrade/getStudentScore/1430
     * https://newbinusmaya.binus.ac.id/services/ci/index.php/scoring/ViewGrade/getStudentScore/1510
     *
     * 1410 = 2014 Odd Semester
     * 1420 = 2014 Even Semester
     * 1430 = 2014 Short Semester
     * 1510 = 2015 Odd Semester
     *
     * All those final links must be called one by one.
     */
    private void handleGrades() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<Terms> terms = realm.where(Terms.class).findAll();
            realm.beginTransaction();
            realm.clear(Grades.class);
            realm.clear(GradesCourse.class);
            for (Terms term:terms) {
                RequestFuture<String> future = RequestFuture.newFuture();
                RequestQueue queue = VolleySingleton.getInstance(this).getQueue();
                queue.add(Request.create(this, getString(R.string.request_grades) + term.getValue(), future,future));
                String data;
                String response = "";
                Log.d("handleGrades", "Called !!!!!!"+term);
                try{
                    response = future.get();
                } catch (Exception e) {}
                JSONObject grade = new JSONObject(response);
                JSONArray arrays = grade.getJSONArray("score");
                for(int j = 0 ; j < arrays.length() ; j++){
                    arrays.getJSONObject(j).put("STRM",term.getValue());
                }
                data = arrays.toString();
                List<Grades> grades = GsonHelper.create().fromJson(data, new TypeToken<List<Grades>>() {
                }.getType());
                List<GradesCourse> course = GsonHelper.create().fromJson(data, new TypeToken<List<GradesCourse>>() {
                }.getType());
                realm.copyToRealm(grades);
                realm.copyToRealmOrUpdate(course);
            }
            realm.commitTransaction();
        } catch (JSONException e) {
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }finally {
            realm.close();
        }
    }

    private void handleTerms() {
        RequestFuture<String> future = RequestFuture.newFuture();
        RequestQueue queue = VolleySingleton.getInstance(this).getQueue();
        queue.add(Request.create(this, getString(R.string.request_terms), future, future));
        String response = "";
        try{
            response = future.get();
        } catch (Exception e) {}
        try {
            List<Terms> terms = GsonHelper.create().fromJson(response, new TypeToken<List<Terms>>() {
            }.getType());
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(terms);
            realm.commitTransaction();
            realm.close();
        } catch (JsonSyntaxException e) {
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }
    }

    private void handleCourse() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<Terms> terms = realm.where(Terms.class).findAll();
            realm.beginTransaction();
            for (Terms term:terms) {
                RequestFuture<String> future = RequestFuture.newFuture();
                RequestQueue queue = VolleySingleton.getInstance(this).getQueue();
                queue.add(Request.create(this, getString(R.string.request_course) + term.getValue(), future,future));
                String data;
                String response = "";
                Log.d("handleCourse", "Called !!!!!!"+term);
                try{
                    response = future.get();
                } catch (Exception e) {}
                JSONObject grade = new JSONObject(response);
                JSONArray arrays = grade.getJSONArray("Courses");
                for(int j = 0 ; j < arrays.length() ; j++){
                    arrays.getJSONObject(j).put("STRM",term.getValue());
                }
                data = arrays.toString();
                List<Course> courses = GsonHelper.create().fromJson(data, new TypeToken<List<Course>>() {
                }.getType());
                realm.copyToRealmOrUpdate(courses);
            }
            realm.commitTransaction();
        } catch (JSONException e) {
            stopSelf();
            EventBus.getDefault().post(new UpdateFailedEvent());
        }finally {
            realm.close();
        }
    }

    private void handleAccount(){
        //TODO Migrate FetchAccountData to update service
    }

    @Override
    public void onDestroy() {
        isActive = false;
        Log.d("Ondestroy", "Called !!!!!!");
        EventBus.getDefault().post(new UpdateFinishEvent());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Pref.save(getApplication(),getString(R.string.last_update_pref),sdf.format(new Date()));
        super.onDestroy();
    }
}
