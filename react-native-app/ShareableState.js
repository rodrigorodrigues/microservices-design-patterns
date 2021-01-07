import { useState, useCallback } from 'react';
import { useBetween } from 'use-between';

const shareableState = () => {
  const [jwt, setJwt] = useState();
  const [name, setName] = useState();
  const [autenticated, setAutenticated] = useState(false);

  const isLogged = useCallback(
    (user) => {
      const token = user.id_token;
      if (token) {
          setJwt(user.id_token);
      }
      return token !== undefined;
    },
    [jwt]
  );

  return {
    jwt,
    setJwt,
    name,
    setName,
    autenticated,
    setAutenticated
  };
};

export const useShareableState = () => useBetween(shareableState);