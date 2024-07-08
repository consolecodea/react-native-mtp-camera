import React, { useEffect, useState } from 'react';
import { View, Text, PermissionsAndroid, Image } from 'react-native';
import {
  startService,
  onNewImage,
  stopService,
} from '@consolecodea/react-native-mtp-camera';

const MyComponent = () => {
  const [image, setImage] = useState<string>('');
  useEffect(() => {
    loadImagesFromDevice();
    onNewImage((e: any) => setImage(`data:image/jpeg;base64,${e?.imageData}`));
  }, []);

  const loadImagesFromDevice = async () => {
    try {
      let res = await startService();
      console.log('Image loading service started.' + res);
    } catch (error) {
      console.error('Failed to start image loading service:', error);
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <Image
        source={{ uri: image }}
        style={{ height: '100%', width: '100%' }}
        resizeMode="contain"
      />
    </View>
  );
};

export default MyComponent;
