import React from 'react';
import { RotatingLines } from 'react-loader-spinner';

export function loading(isLoading) {
    console.log("render:loading: " + isLoading);

    return (isLoading ?
        <div>
                <RotatingLines
                    strokeColor="#f1f1f1"
                    strokeWidth="5"
                    animationDuration="0.75"
                    width="96"
                    visible={true}
                />
        </div>
            : '');
}