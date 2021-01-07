import React, { useEffect } from 'react';
import { Text, Platform } from 'react-native';
import { colors, ThemeProvider } from 'react-native-elements';
import { showMessage } from "react-native-flash-message";
import { useShareableState } from './ShareableState';

const theme = {
  colors: {
    ...Platform.select({
      default: colors.platform.android,
      ios: colors.platform.ios,
    }),
  },
};

export default function Home() {
   const { setJwt, setAutenticated, autenticated } = useShareableState();

    useEffect(() => {
        const url = `${process.env.API_GATEWAY_URL}/api/authenticatedUser`;
        fetch(url, {
                credentials: 'include'
            })
            .then((response) => response.json())
            .then((responseJson) => {
                console.log("response: ", responseJson);
                if (responseJson.status) {
                    showMessage({
                        message: "Something went wrong",
                        description: responseJson.message,
                        type: "danger",
                        icon: "auto"
                    });
                    setAutenticated(false);
                } else {
                    setJwt(responseJson.id_token);
                    setAutenticated(true);
                }
                console.log("autenticated: "+autenticated);
        })
        .catch((error) => {
            console.log("Something went wrong: ", error);
        });
    }, []);


    return (
    <ThemeProvider theme={theme}>
        <Text>Home</Text>
    </ThemeProvider>);
};