import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Router, Route, Link } from './react-router';
import Home from './Home';
import Receipt from './Receipt';
import Login from './Login';
import FlashMessage from "react-native-flash-message";

const About = () => <Text>About</Text>;

const App = () => (
  <Router>
    <View style={styles.container}>
      <View style={styles.nav}>
        <Link to="/">
          <Text>Home</Text>
        </Link>
        <Link to="/receipt">
          <Text>Receipt</Text>
        </Link>
        <Link to="/login">
          <Text>Login</Text>
        </Link>
        <Link to="/about">
          <Text>About</Text>
        </Link>
      </View>

      <Route exact path="/" component={Home} />
      <Route path="/receipt" component={Receipt} />
      <Route path="/login" component={Login} />
      <Route path="/about" component={About} />
      <FlashMessage position="bottom" />
    </View>
  </Router>
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
