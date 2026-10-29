import {createSlice} from "@reduxjs/toolkit";

interface UiState {
    sidebarOpen: boolean;
}

const initialState: UiState = {
    sidebarOpen: true,
}

const uiSlice = createSlice({
    name: "ui",
    initialState,
    reducers: {
        toggleSidebar(state) {
            state.sidebarOpen = !state.sidebarOpen;
        },
    }
})

export const {toggleSidebar} = uiSlice.actions;

export const uiReducer = uiSlice.reducer;