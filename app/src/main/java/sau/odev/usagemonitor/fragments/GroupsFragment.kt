package sau.odev.usagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.ui.GroupsAdapter
import java.util.concurrent.Executors

class GroupsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: ExtendedFloatingActionButton
    private lateinit var groupsAdapter: GroupsAdapter
    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        addButton = view.findViewById(R.id.add_btn)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        groupsAdapter = GroupsAdapter(
            onEditClick = { group -> (activity as? GroupEditListener)?.showEditGroupDialog(group) },
            onDeleteClick = { group -> (activity as? GroupEditListener)?.deleteGroup(group) }
        )

        recyclerView.adapter = groupsAdapter

        addButton.setOnClickListener {
            (activity as? GroupEditListener)?.showEditGroupDialog(null)
        }

        loadData()
    }

    fun loadData() {
        executor.execute {
            val groups = database.getGroupsDao().getGroups()

            activity?.runOnUiThread {
                groupsAdapter.updateData(groups)
            }
        }
    }

    interface GroupEditListener {
        fun showEditGroupDialog(group: Group?)
        fun deleteGroup(group: Group)
    }

    companion object {
        fun newInstance() = GroupsFragment()
    }
}

