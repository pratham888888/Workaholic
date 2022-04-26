package com.pratham.workaholic.activities

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pratham.workaholic.R
import com.pratham.workaholic.adapters.MemberListItemsAdapter
import com.pratham.workaholic.firebase.FirestoreClass
import com.pratham.workaholic.models.Board
import com.pratham.workaholic.models.User
import com.pratham.workaholic.utils.Constants
import kotlinx.android.synthetic.main.activity_members.*
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.dialog_search_member.*
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


class MembersActivity : BaseActivity() {

    // A global variable for Board Details.
    private lateinit var mBoardDetails: Board

    // A global variable for Assigned Members List.
    private lateinit var mAssignedMembersList: ArrayList<User>

    // A global variable for notifying any changes done or not in the assigned members list.
    private var anyChangesDone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_members)

        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }

        setupActionBar()

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(
            this@MembersActivity,
            mBoardDetails.assignedTo
        )
    }

    override fun onBackPressed() {
        if (anyChangesDone) {
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    /**
     * A function to setup action bar
     */
    private fun setupActionBar() {

        setSupportActionBar(toolbar_members_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
        }

        toolbar_members_activity.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu to use in the action bar
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.action_add_member -> {

                dialogSearchMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * A function to setup assigned members list into recyclerview.
     */
    fun setupMembersList(list: ArrayList<User>) {

        mAssignedMembersList = list

        hideProgressDialog()

        rv_members_list.layoutManager = LinearLayoutManager(this@MembersActivity)
        rv_members_list.setHasFixedSize(true)

        val adapter = MemberListItemsAdapter(this@MembersActivity, list)
        rv_members_list.adapter = adapter
    }

    /**
     * Method is used to show the Custom Dialog.
     */
    private fun dialogSearchMember() {
        val dialog = Dialog(this)
        /*Set the screen content from a layout resource.
    The resource will be inflated, adding all top-level views to the screen.*/
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.tv_add.setOnClickListener(View.OnClickListener {

            val email = dialog.et_email_search_member.text.toString()

            if (email.isNotEmpty()) {
                dialog.dismiss()

                // Show the progress dialog.
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this@MembersActivity, email)
            } else {
                showErrorSnackBar("Please enter members email address.")
                /*Toast.makeText(
                    this@MembersActivity,
                    "Please enter members email address.",
                    Toast.LENGTH_SHORT
                ).show()*/
            }
        })
        dialog.tv_cancel.setOnClickListener(View.OnClickListener {
            dialog.dismiss()
        })
        //Start the dialog and display it on screen.
        dialog.show()
    }

    fun memberDetails(user: User) {

        mBoardDetails.assignedTo.add(user.id)

        FirestoreClass().assignMemberToBoard(this@MembersActivity, mBoardDetails, user)
    }

    /**
     * A function to get the result of assigning the members.
     */
    fun memberAssignSuccess(user: User) {

        hideProgressDialog()

        mAssignedMembersList.add(user)

        anyChangesDone = true

        setupMembersList(mAssignedMembersList)
        sendNotificationToUserAsyncTask(mBoardDetails.name,user.fcmToken).execute()
    }
    private inner class sendNotificationToUserAsyncTask(val boardName:String,val token:String):
        AsyncTask<Any,Void,String>() {
        override fun onPreExecute() {
            //show progress dialog
            super.onPreExecute()

        }
        override fun doInBackground(vararg params: Any?): String {
           var result:String
           var connection:HttpURLConnection? =null
            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection =url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput =true
                connection.instanceFollowRedirects =false
                connection.requestMethod= "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(
                    Constants.FCM_AUTHORIZATION
                    ,"${Constants.FCM_KEY} =${Constants.FCM_SERVER_KEY}"
                )
                connection.useCaches=false

                val wr =DataOutputStream(connection.outputStream)
                val jsonRequest =JSONObject()
                val dataObject=JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE,"Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE,"You have been assigned to the new board by" +
                        " ${mAssignedMembersList[0].name}")
                jsonRequest.put(Constants.FCM_KEY_DATA,dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO,token)
                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult:Int =connection.responseCode
                if(httpResult ==HttpURLConnection.HTTP_OK){
                    val inputStream=connection.inputStream
                    val reader =BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line:String?
                    try{
                        while(reader.readLine().also {line=it}!=null){
                            sb.append(line+"\n")

                        }
                    }catch(e:IOException){
                        e.printStackTrace()
                    }finally {
                        try{
                            inputStream.close()
                        }catch (e:IOException){
                            e.printStackTrace()
                        }
                    }
                result= sb.toString()
                }else{
                    result= connection.responseMessage
                }


            }catch (e:SocketTimeoutException){
                result="Connection timeout"
            }catch(e:Exception){
                result ="Error:"+e.message
            }finally {
                connection?.disconnect()
            }
           return result
        }

        override fun onPostExecute(result: String?) {
            //hide progress dialog
            super.onPostExecute(result)
        }
    }
}