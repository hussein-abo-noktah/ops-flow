import {createSlice} from "@reduxjs/toolkit";

interface UiState {
    sidebarOpen: boolean;
    mobileSidebarOpen: boolean;
}

const initialState: UiState = {
    sidebarOpen: true,
    mobileSidebarOpen: false,
}

const uiSlice = createSlice({
    name: "ui",
    initialState,
    reducers: {
        toggleSidebar(state) {
            state.sidebarOpen = !state.sidebarOpen;
        },
        openMobileSidebar(state) { state.mobileSidebarOpen = true; },
        closeMobileSidebar(state) { state.mobileSidebarOpen = false; },
    }
})

export const {toggleSidebar, openMobileSidebar, closeMobileSidebar} = uiSlice.actions;

export const uiReducer = uiSlice.reducer;
