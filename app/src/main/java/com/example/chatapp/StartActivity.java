package com.example.chatapp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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


public class StartActivity extends AppCompatActivity {

    RecyclerView contact_recyclerview;
    List<Contact> contactList;
    ContactAdapter contactAdapter;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Map<String, Object> cont = new HashMap<>();
    HashMap <String, String> datac = new HashMap<String, String>();
    int x = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        contact_recyclerview = findViewById(R.id.rv);
        contact_recyclerview.setHasFixedSize(true);
        contact_recyclerview.setLayoutManager(new LinearLayoutManager(this));

        contactList = new ArrayList<>();

        contactAdapter = new ContactAdapter(this, contactList);
        contact_recyclerview.setAdapter(contactAdapter);
        contact_recyclerview.addItemDecoration(new DividerItemDecoration(StartActivity.this, DividerItemDecoration.VERTICAL));


    }

    void getContacts(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},1);
        }else{
            getContactList();
        }
    }

    private void getContactList() {

         Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, null,null, null);
        while(cursor.moveToNext()){
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Contact contact = new Contact(name, number);
            contactList.add(contact);
            contactAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       if(requestCode==1){
           if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
               getContactList();
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

         //  saveData();
           if(x==0)
           {
               contactList.clear();
               readData();
               x++;
           }
        }
        if (id == R.id.action_sync_contact) {
            contactList.clear();
            x=0;
           getContacts();

        }
        return super.onOptionsItemSelected(item);
    }

    private void readData() {
        db.collection("contacts").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                String name = document.getString("name");
                                String num = document.getString("num");
//                                Log.d("fetch", name);
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
    }

    private void saveData() {

        HashMap<String, Contact> d = new HashMap<String, Contact>();

        for(Contact contact : contactList){
            d.put(contact.getName(), contact);
        }

       db.collection("contacts").add(d).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
           @Override
           public void onSuccess(DocumentReference documentReference) {
               Log.d("added", "DocumentSnapshot added with ID: " + documentReference.getId());
           }
       }).addOnFailureListener(new OnFailureListener() {
           @Override
           public void onFailure(@NonNull Exception e) {
               Log.w("added", "Error adding document", e);
           }
       });
    }


}
