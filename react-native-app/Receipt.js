import React, { useState, useEffect } from 'react';
import { useShareableState } from './ShareableState';

// import all the components we are going to use
import { SafeAreaView, Text, StyleSheet, View, FlatList, Linking } from 'react-native';
import { SearchBar } from 'react-native-elements';

const Receipt = () => {
  const [search, setSearch] = useState('');
  const [filteredDataSource, setFilteredDataSource] = useState([]);
  const [masterDataSource, setMasterDataSource] = useState([]);
  const [ redirect, setRedirect ] = useState(false);
  const { getJwt, isLogged } = useShareableState();

  useEffect(() => {
    if (!isLogged()) {
      Linking.openURL('/');
    } else {
      fetch(`${process.env.API_GATEWAY_URL}/api/tasks`, {
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': getJwt()
        }
      })
      .then((response) => response.json())
      .then((responseJson) => {
        setFilteredDataSource(responseJson.content);
        setMasterDataSource(responseJson.content);
      })
      .catch((error) => {
        console.error(error);
      });
    }
  }, []);

  const searchFilterFunction = (text) => {
    // Check if searched text is not blank
    if (text) {
      // Inserted text is not blank
      // Filter the masterDataSource
      // Update FilteredDataSource
      const newData = masterDataSource.filter(function (item) {
        const itemData = item.name
          ? item.name.toUpperCase() + item.id
          : ''.toUpperCase();
        const textData = text.toUpperCase();
        return itemData.indexOf(textData) > -1;
      });
      setFilteredDataSource(newData);
      setSearch(text);
    } else {
      // Inserted text is blank
      // Update FilteredDataSource with masterDataSource
      setFilteredDataSource(masterDataSource);
      setSearch(text);
    }
  };

  const ItemView = ({ item }) => {
    return (
      // Flat List Item
      <Text style={styles.itemStyle} onPress={() => getItem(item)}>
        {item.id}
        {'.'}
        {item.name.toUpperCase()}
      </Text>
    );
  };

  const ItemSeparatorView = () => {
    return (
      // Flat List Item Separator
      <View
        style={{
          height: 0.5,
          width: '100%',
          backgroundColor: '#C8C8C8',
        }}
      />
    );
  };

  const getItem = (item) => {
    // Function for click on an item
    console.log(`Item: ${item}`);
    alert('Id : ' + item.id + ' Title : ' + item.name);
  };

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={styles.container}>
        <SearchBar
          round
          searchIcon={{ size: 24 }}
          onChangeText={(text) => searchFilterFunction(text)}
          onClear={(text) => searchFilterFunction('')}
          placeholder="Type Here..."
          value={search}
        />
        <FlatList
          data={filteredDataSource}
          keyExtractor={(item, index) => index.toString()}
          ItemSeparatorComponent={ItemSeparatorView}
          renderItem={ItemView}
        />
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'white',
  },
  itemStyle: {
    padding: 10,
  },
});

export default Receipt;