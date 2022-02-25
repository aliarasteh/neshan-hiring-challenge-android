package org.neshan.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.neshan.R;
import org.neshan.databinding.BottomsheetLocationDetailBinding;

public class LocationDetailBottomSheet extends BottomSheetDialogFragment {

    private BottomsheetLocationDetailBinding mBinding;

    private MainActivityViewModel mViewModel;

    // trigger action when bottom sheet closes
    private DialogInterface.OnDismissListener mOnDismissListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // set bottom sheet theme
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mBinding = BottomsheetLocationDetailBinding.inflate(getLayoutInflater(), container, false);

        return mBinding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Dialog dialog = getDialog();
        if (dialog != null) {
            // avoid default dim background for bottom sheet
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // when bottom sheet closed -> trigger dismiss listener
            dialog.setOnDismissListener(d -> {
                this.dismiss();
                if (mOnDismissListener != null) mOnDismissListener.onDismiss(d);
            });
        }

        mViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        // bind data to view
        mBinding.setAddressDetail(mViewModel.getLocationAddressDetailLiveData().getValue());

        setViewListeners();

    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    private void setViewListeners() {

        mBinding.route.setOnClickListener(view1 -> {
            if (getDialog() != null) {
                getDialog().dismiss();
            }
        });

    }
}
