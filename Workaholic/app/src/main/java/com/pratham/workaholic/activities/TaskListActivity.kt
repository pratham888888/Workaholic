package com.pratham.workaholic.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.pratham.workaholic.R
import com.pratham.workaholic.adapters.TaskListItemsAdapter
import com.pratham.workaholic.firebase.FirestoreClass
import com.pratham.workaholic.models.Board
import com.pratham.workaholic.models.Card
import com.pratham.workaholic.models.Task
import com.pratham.workaholic.models.User
import com.pratham.workaholic.utils.Constants
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.activity_task_list.*
import java.text.FieldPosition
class TaskListActivity : BaseActivity() {

    // A global variable for Board Details.
    private lateinit var mBoardDetails: Board
    lateinit var mAssignedMemberDetailsList:ArrayList<User>

    // A global variable for board document id as mBoardDocumentId
    private lateinit var mBoardDocumentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            mBoardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this@TaskListActivity, mBoardDocumentId)
    }

    /**
     * A function to setup action bar
     */
    private fun setupActionBar() {

        setSupportActionBar(toolbar_task_list_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
            actionBar.title = mBoardDetails.name
        }

        toolbar_task_list_activity.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu to use in the action bar
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.action_members -> {

                val intent = Intent(this@TaskListActivity, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                startActivityForResult(intent, MEMBERS_REQUEST_CODE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO (Step 7: Get the success result from Card Details Activity.)
        // START
        if (resultCode == Activity.RESULT_OK
            && (requestCode == MEMBERS_REQUEST_CODE || requestCode == CARD_DETAILS_REQUEST_CODE)
        ) {
            // Show the progress dialog.
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardDetails(this@TaskListActivity, mBoardDocumentId)
        }
        // END
        else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    /**
     * A function to get the result of Board Detail.
     */
    fun boardDetails(board: Board) {

        mBoardDetails = board

        hideProgressDialog()

        // Call the function to setup action bar.
        setupActionBar()

        // Here we are appending an item view for adding a list task list for the board.
      // Attach the adapter to the recyclerView.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)
    }

    /**
     * A function to get the task list name from the adapter class which we will be using to create a new task list in the database.
     */
    fun createTaskList(taskListName: String) {

        Log.e("Task List Name", taskListName)

        // Create and Assign the task details
        val task = Task(taskListName, FirestoreClass().getCurrentUserID())

        mBoardDetails.taskList.add(0, task) // Add task to the first position of ArrayList
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1) // Remove the last position as we have added the item manually for adding the TaskList.

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    /**
     * A function to update the taskList
     */
    fun updateTaskList(position: Int, listName: String, model: Task) {

        val task = Task(listName, model.createdBy)

        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    /**
     * A function to delete the task list from database.
     */
    fun deleteTaskList(position: Int) {

        mBoardDetails.taskList.removeAt(position)

        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    /**
     * A function to get the result of add or updating the task list.
     */
    fun addUpdateTaskListSuccess() {

        hideProgressDialog()

        // Here get the updated board details.
        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this@TaskListActivity, mBoardDetails.documentId)
    }

    /**
     * A function to create a card and update it in the task list.
     */
    fun addCardToTaskList(position: Int, cardName: String) {

        // Remove the last item
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        val cardAssignedUsersList: ArrayList<String> = ArrayList()
        cardAssignedUsersList.add(FirestoreClass().getCurrentUserID())

        val card = Card(cardName, FirestoreClass().getCurrentUserID(), cardAssignedUsersList)

        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList
        )

        mBoardDetails.taskList[position] = task

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@TaskListActivity, mBoardDetails)
    }

    /**
     * A function for viewing and updating card details.
     */
    fun cardDetails(taskListPosition: Int, cardPosition: Int) {
        val intent = Intent(this@TaskListActivity, CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST,mAssignedMemberDetailsList)
        // TODO (Step 6: Update the intent using the start activity for result.)
        // START
        startActivityForResult(intent, CARD_DETAILS_REQUEST_CODE)
        // END
    }
     fun boardMembersDetailsList(list:ArrayList<User>){
         mAssignedMemberDetailsList= list
         hideProgressDialog()
         val addTaskList = Task(resources.getString(R.string.add_list))
         mBoardDetails.taskList.add(addTaskList)

         rv_task_list.layoutManager =
             LinearLayoutManager(this@TaskListActivity, LinearLayoutManager.HORIZONTAL, false)
         rv_task_list.setHasFixedSize(true)

         // Create an instance of TaskListItemsAdapter and pass the task list to it.
         val adapter = TaskListItemsAdapter(this@TaskListActivity, mBoardDetails.taskList)
         rv_task_list.adapter = adapter
     }
    /**
     * A companion object to declare the constants.
     */


    fun updateCardsInTaskList(taskListPosition: Int,cards:ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        mBoardDetails.taskList[taskListPosition].cards =cards
        //show progress dialog
    FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }
    companion object {
        //A unique code for starting the activity for result
        const val MEMBERS_REQUEST_CODE: Int = 13

        // TODO (Step 5: Add a unique request code for starting the activity for result.)
        // START
        const val CARD_DETAILS_REQUEST_CODE: Int = 14
        // END
    }
}