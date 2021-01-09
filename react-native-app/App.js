import React, { useState } from 'react';
import { StyleSheet, Text, View, Linking } from 'react-native';
import { Router, Route, Link } from './react-router';
import Home from './Home';
import Receipt from './Receipt';
import Login from './Login';
import FlashMessage from "react-native-flash-message";
import { useShareableState } from './ShareableState';
import { Header } from 'react-native-elements';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';

const About = () => <View>
    <Text>About</Text>
    <Text>Environment - {process.env.API_GATEWAY_URL}</Text>
  </View>;

function logout() {
    const { logout } = useShareableState();
    fetch(`${process.env.API_GATEWAY_URL}/logout`, {
            credentials: 'include'
        })
        .then((response) => response.json())
        .then((responseJson) => {
            console.log("response: ", responseJson);
    })
    .catch((error) => {
      showMessage({
        message: "Something went wrong",
        description: error,
        type: "danger",
        icon: "auto"
      });
    });
    logout();
    Linking.openURL('/');
}

const DisplayLogin = () => {
  const { isLogged } = useShareableState();
  if (!isLogged()) {
    return <Link to="/login">
      <Text>Login</Text>
    </Link>
  } else {
    return <Link to="/logout">
      <Text>Logout</Text>
    </Link>
  }
};

const DisplayReceipt = () => {
  const { isLogged } = useShareableState();
  if (isLogged()) {
    return <Link to="/receipt">
    <Text>Receipt</Text>
  </Link>
  } else {
    return null;
  }
}

const App = () => (
  <SafeAreaProvider>
    <NavigationContainer>
    <Router>
    <View style={styles.container}>
      <View style={styles.nav}>
        <Link to="/">
          <Text>Home</Text>
        </Link>
        <DisplayLogin />
        <DisplayReceipt />
        <Link to="/about">
          <Text>About</Text>
        </Link>
      </View>

      <Route exact path="/" component={Home} />
      <Route path="/receipt" component={Receipt} />
      <Route path="/login" component={Login} />
      <Route path="/logout" component={() => logout()} />
      <Route path="/about" component={About} />
      <FlashMessage position="bottom" />
    </View>
  </Router>

    </NavigationContainer>
  </SafeAreaProvider>
);

const styles = StyleSheet.create({
  container: {
    marginTop: 25,
    padding: 10
  },
  nav:{
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
});

export default App;
