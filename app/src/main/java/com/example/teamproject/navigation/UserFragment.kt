package com.example.teamproject.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.teamproject.LoginActivity
import com.example.teamproject.MainActivity
import com.example.teamproject.R
import com.example.teamproject.databinding.FragmentUserBinding
import com.example.teamproject.navigation.model.AlarmDTO
import com.example.teamproject.navigation.model.ContentDTO
import com.example.teamproject.navigation.model.FollowDTO
import com.example.teamproject.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {
    val binding = FragmentUserBinding.inflate(layoutInflater)
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid: String? = null //identify the id, if the id's mine or not(others)

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid  //identify the id, if the id's mine or not(others)

        if (uid == currentUserUid) { //if user's id identical with current user's id
            // My page
            binding.accountBtnFollowSignout.text = getString(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
            //others fragment page
        } else {
            binding.accountBtnFollowSignout.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            //mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity.binding.toolbarBtnBack.setOnClickListener {
                mainactivity.binding.bottomNavigation.selectedItemId = R.id.action_home
            }
            mainactivity.binding.toolbarTitleImage.visibility = View.GONE
            //mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity.binding.toolbarBtnBack.visibility = View.VISIBLE
            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }

            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }
        }

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 2)

        binding.accountRecyclerview.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        binding.accountIvProfile.setOnClickListener {          //changing profile image function
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null) {
                binding.accountTvFollowingCount.text = followDTO.followingCount.toString()
            }
            if(followDTO?.followingCount != null){
                binding.accountTvFollowerCount.text = followDTO.followerCount.toString()
                if(followDTO.followers.containsKey(currentUserUid!!)) {
                    binding.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                    binding.accountBtnFollowSignout.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        binding.accountBtnFollowSignout.text = getString(R.string.follow)
                        binding.accountBtnFollowSignout.background?.colorFilter = null
                    }
                }
            }
        }
    }
    fun requestFollow() {
        //Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)) {
                //remove following third person when the third person already follows me
                followDTO?.followingCount = followDTO?.followingCount - 1
            }else {
                //remove following third person when the third person already follows me
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followers[uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        //Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)) {
                //cancel my follower when I already follow the third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else {
                //add my follower when I don't follow the third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow )
        FcmPush.instance.sendMessage(destinationUid, "Dear Diary", message)
    }

    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if (value == null) return@addSnapshotListener
            if (value.data != null) {
                var url = value?.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(binding.accountIvProfile)
            }
        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { value, error ->
                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    binding.accountTvPostCount.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var width = resources.displayMetrics.widthPixels / 2

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView) {
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.imageView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
    }
}