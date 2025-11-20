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
import sau.odev.usagemonitor.ui.ChallengesAdapter
import java.util.concurrent.Executors

class ChallengesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: ExtendedFloatingActionButton
    private lateinit var challengesAdapter: ChallengesAdapter
    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_challenges, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        addButton = view.findViewById(R.id.add_btn)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        challengesAdapter = ChallengesAdapter()

        recyclerView.adapter = challengesAdapter

        addButton.setOnClickListener {
            (activity as? ChallengeCreateListener)?.showCreateChallengeDialog()
        }

        loadData()
    }

    fun loadData() {
        executor.execute {
            val apps = database.getAppsDao().getAllApps()
            val groups = database.getGroupsDao().getGroups()
            val appChallenges = database.getChallengesDao().getAppsChallenges()
            val groupChallenges = database.getChallengesDao().getGroupsChallenges()
            val deviceFastChallenges = database.getChallengesDao().getDeviceFastChallenges()

            activity?.runOnUiThread {
                challengesAdapter.updateData(
                    appChallenges,
                    groupChallenges,
                    deviceFastChallenges,
                    apps,
                    groups
                )
            }
        }
    }

    interface ChallengeCreateListener {
        fun showCreateChallengeDialog()
    }

    companion object {
        fun newInstance() = ChallengesFragment()
    }
}

