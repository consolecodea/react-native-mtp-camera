import { StyleSheet, View, Text } from 'react-native';
import { multiply } from '@consolecodea/react-native-mtp-camera';
import React, { useState } from 'react';

export default function App() {
  const [result, setResult] = useState(0);

  React.useEffect(() => {
    let res = multiply(2, 4);
    setResult(res);
  }, []);
  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
