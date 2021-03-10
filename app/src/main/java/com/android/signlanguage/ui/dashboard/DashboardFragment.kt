package com.android.signlanguage.ui.dashboard

//import com.android.signlanguage.ml.Model
import android.content.res.AssetManager
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.random.Random


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

        val handsLandmarks = getMultiHandLandmarksDebugString(HomeFragment.letterA)
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