package sau.odev.usagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.ui.AppsAdapter
import java.util.concurrent.Executors

class AppsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    private var allApps: List<App> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        searchView = view.findViewById(R.id.search_view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        appsAdapter = AppsAdapter(requireActivity().packageManager) { app ->
            (activity as? AppEditListener)?.showEditAppDialog(app)
        }

        recyclerView.adapter = appsAdapter

        setupSearchView()
        loadData()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })
    }

    private fun filterApps(query: String) {
        val filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                app.name.contains(query, ignoreCase = true) ||
                app.category.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            }
        }

        executor.execute {
            val groups = database.getGroupsDao().getGroups()
            activity?.runOnUiThread {
                appsAdapter.updateData(filteredApps, groups)
            }
        }
    }

    fun loadData() {
        executor.execute {
            val apps = database.getAppsDao().getAllApps().sortedBy { it.name }
            val groups = database.getGroupsDao().getGroups()

            allApps = apps

            activity?.runOnUiThread {
                appsAdapter.updateData(apps, groups)
            }
        }
    }

    interface AppEditListener {
        fun showEditAppDialog(app: App)
    }

    companion object {
        fun newInstance() = AppsFragment()
    }
}

