import React from 'react';
import ReactDOM from 'react-dom/client';
import {createRoot} from 'react-dom/client';
import './index.css';
import App from './App';
import * as serviceWorker from './serviceWorker';
import 'bootstrap/dist/css/bootstrap.min.css';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.min.css'
import '@trendmicro/react-sidenav/dist/react-sidenav.css';
import 'rc-footer/assets/index.css';
import '@trendmicro/react-breadcrumbs/dist/react-breadcrumbs.css';

// Call it once in your app. At the root of your app is the best place
toast.configure({
    draggable: false, 
    hideProgressBar: true,
    style: {
        width: "50%"
    }
});

const container = document.getElementById('root');
const root = createRoot(container); 
root.render(<App />);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
