package com.example.chatapp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class StartActivity extends AppCompatActivity {

    RecyclerView contact_recyclerview;
    List<Contact> contactList;
    ContactAdapter contactAdapter;

    List<Contact> contactListFirebase;
    List<Contact> contactListPhoneContact;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
   

    HashMap <String, String> datac = new HashMap<String, String>();
    
    int x = 0;
    ProgressDialog TempDialog ;
    CountDownTimer countDownTimer;
    int t=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TempDialog = new ProgressDialog(StartActivity.this);
        TempDialog.setMessage("Loading...");
        TempDialog.setCancelable(false);
        TempDialog.setProgress(t);
        TempDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        TempDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));

        contact_recyclerview = findViewById(R.id.rv);
        contact_recyclerview.setHasFixedSize(true);
        contact_recyclerview.setLayoutManager(new LinearLayoutManager(this));

        contactList = new ArrayList<>();   // contact list to be shown on App contact list.

        contactListFirebase = new ArrayList<>();   // contact list to be shown on App contact list after clicking on syncing from firebase .

        contactListPhoneContact = new ArrayList<>();   // contact list to be shown on App contact list after clicking on syncing from Phone contact . 

        getContacts(1);        // ReadContactsFromPhone

        readDataFirebase(1);  // ReadContactFromFirebase

        TempDialog.show();     // Created Dialog box to wait after fetching contact from firebase
        countDownTimer = new CountDownTimer(4000, 3000) {
            @Override
            public void onTick(long millisUntilFinished) {
                   TempDialog.setMessage("Loading...");
            }

            @Override
            public void onFinish() {
                TempDialog.dismiss();
               compareContacts();   // comparing both contact list
            }
        }.start();

        contactAdapter = new ContactAdapter(this, contactList);

        contact_recyclerview.setAdapter(contactAdapter);
        contact_recyclerview.addItemDecoration(new DividerItemDecoration(StartActivity.this, DividerItemDecoration.VERTICAL));


    }


    private void compareContacts() {
        Log.d("compared", "I m called");

        contactList = intersectionList(contactListFirebase, contactListPhoneContact);

        Log.d(" After compared", contactList.toString());
      //  contactAdapter.notifyDataSetChanged();
    }

    public List<Contact> intersectionList(List<Contact> list1, List<Contact> list2) {
        Log.d("Intersection Called ", "I m called");
        List<Contact> list = new ArrayList<Contact>();

        for (Contact t : list1) {
           // Log.d("compared", t.getName()+" : "+t.getClass());
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getContactList(1);
            }
        }
    }

    void getContacts(int x){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},1);
        }else{
            getContactList(x);
        }
    }

    private void getContactList(int t) {

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, null,null, null);
        while(cursor.moveToNext()){
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Contact contact = new Contact(name, number);
            Log.d("fetch", contact.getName()+":"+contact.getNumber());

            if(t==0)
            {
                contactList.add(contact);
                contactAdapter.notifyDataSetChanged();
            }
            else
            {
                contactListPhoneContact.add(contact);
               // contactAdapter.notifyDataSetChanged();
            }


        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sync_firebase) {
            contactList.clear();
            readDataFirebase(0);
           // Log.d("fetch", contactList.toString());
        }
        if (id == R.id.action_sync_contact) {
            contactList.clear();
            getContacts(0);
           // Log.d("fetch", contactList.toString());
        }
        return super.onOptionsItemSelected(item);
    }

    private void readDataFirebase(int t) {
        if(t==0){
            db.collection("contacts").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {

                                    String name = document.getString("name");
                                    String num = document.getString("num");
                                    Log.d("fetch", name+":"+num);
//                                Log.d("fetch", num);

                                    Contact contact = new Contact(name, num);
                                    contactList.add(contact);
                                    contactAdapter.notifyDataSetChanged();
                                }

                            } else {
                                Log.w("fetch", "Error getting documents.", task.getException());
                            }
                        }
                    });
        }else {

            db.collection("contacts").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {

                                    String name = document.getString("name");
                                    String num = document.getString("num");
                                    Log.d("fetch", name+":"+num);
//                                Log.d("fetch", num);

                                    Contact contact = new Contact(name, num);
                                    contactListFirebase.add(contact);
                                   // contactAdapter.notifyDataSetChanged();
                                }

                            } else {
                                Log.w("fetch", "Error getting documents.", task.getException());
                            }
                        }
                    });
        }
    }

  

}
