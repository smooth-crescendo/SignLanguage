package com.android.signlanguage.ui.dashboard

//import com.android.signlanguage.ml.Model
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.R
//import com.android.signlanguage.ml.Model
import com.android.signlanguage.ui.home.HomeFragment
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)

        val handsLandmarks = getMultiHandLandmarksDebugString(HomeFragment.letter)
        Log.d("HANDS", handsLandmarks)
        textView.text = handsLandmarks

        return root
    }

    private fun getMultiHandLandmarksDebugString(
        handsLandmarks: List<NormalizedLandmarkList>
    ): String {
        if (handsLandmarks.isEmpty()) {
            return "No hand landmarks"
        }
        var handsLandmarksStr = String()
        for (landmarks in handsLandmarks) {
            for (landmark in landmarks.landmarkList) {
                handsLandmarksStr += "${landmark.x},${landmark.y},${landmark.z}\n"
            }
        }
        return handsLandmarksStr
    }
}