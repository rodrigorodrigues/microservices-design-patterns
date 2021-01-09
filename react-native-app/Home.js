import React, { useEffect, useState } from 'react';
import { Platform } from 'react-native';
import { colors, ThemeProvider, Header, Image, Text } from 'react-native-elements';
import { showMessage } from "react-native-flash-message";
import { useShareableState } from './ShareableState';
import { Link } from './react-router';
import Cookies from 'js-cookie';
import jwt_decode from 'jwt-decode';
import { View } from 'react-native';
const moment = require('moment');

const theme = {
  colors: {
    ...Platform.select({
      default: colors.platform.android,
      ios: colors.platform.ios,
    }),
  },
};

export default function Home() {
    const [ user, setUser ] = useState(null);
    const [ expireDate, setExpireDate ] = useState(null);
    const [ authorities, setAuthorities ] = useState(null);
    const [ imageUrl, setImageUrl ] = useState(null);
    const [ refreshToken, setRefreshToken ] = useState(null);
    const { setJwtToken, isLogged, getJwtDecoded } = useShareableState();

    useEffect(() => {
        if (isLogged()) {
            console.log("User is authenticated: "+isLogged());
            const jwtDecoded = getJwtDecoded();
            setJwtDetails(jwtDecoded, jwtDecoded.refresh_token, jwtDecoded.expires_in);
            return;
        }
        const url = `${process.env.API_GATEWAY_URL}/api/authenticatedUser`;
        fetch(url, {
                credentials: 'include',
                headers: {
                    'X-XSRF-TOKEN': Cookies.get('XSRF-TOKEN')
                }
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
                } else {
                    setJwtToken(responseJson);
                    decodeJwt(responseJson);
                }
                console.log("autenticated: "+isLogged());
        })
        .catch((error) => {
            console.log("Something went wrong: ", error);
        });
    }, []);

    function decodeJwt(token) {
        const jwtDecoded = jwt_decode(token.access_token);
        setJwtDetails(jwtDecoded, token.refresh_token, token.expires_in);
    }

    function setJwtDetails(jwtDecoded, refreshToken, expiresIn) {
        setUser(jwtDecoded.fullName !== undefined ? jwtDecoded.fullName : jwtDecoded.sub);
        setExpireDate(moment().add(expiresIn, 'seconds').toDate());
        setAuthorities(jwtDecoded.authorities);
        setImageUrl(jwtDecoded.imageUrl);
        setRefreshToken(refreshToken);
    }

    const DisplayHome = () => {
        if (isLogged()) {
            return <View>
                <Text h3>Welcome <b>{user}</b></Text>
                <Text>User Details</Text>
                {imageUrl && <Image source={{ uri: imageUrl }} style={{ width: 200, height: 200 }} />}
                {authorities && <Text h3>Authorities: <b>{authorities}</b></Text> }
            </View>
        } else {
            return <View>
                <Link to="/login">
                  <Text>Please Login</Text>
                </Link>
            </View>;
        }
    }

    return (
    <ThemeProvider theme={theme}>
        <Header
          placement="left"
          leftComponent={{ icon: 'menu', color: '#fff' }}
          centerComponent={{ text: 'Spending Better', style: { color: '#fff' } }}
          rightComponent={{ icon: 'home', color: '#fff' }}
        />        
        <DisplayHome />
    </ThemeProvider>);
};