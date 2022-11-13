import React from 'react';
import LoadingScreen from "react-loading-screen";

export function loading({isLoading}) {
    console.log("render:loading: " + isLoading);

    return (isLoading ?
        <div>
            <LoadingScreen
                loading={true}
                bgColor="#f1f1f1"
                spinnerColor="#9ee5f8"
                textColor="#676767"
                logoSrc="Spinner.gif"
                text="Loading..."
            />
        </div>
            : '');
}