import { useState, useCallback } from 'react';
import { useBetween } from 'use-between';

const shareableState = () => {
  const [jwtDecoded, setJwtDecoded] = useState(null);
  const [jwt, setJwt] = useState();
  const [name, setName] = useState();
  const [autenticated, setAutenticated] = useState(false);

  const isLogged = useCallback(() => {
    return autenticated;
  }, [autenticated]);

  const getJwt = useCallback(() => {
    return jwt;
  }, [jwt]);

  const getJwtDecoded = useCallback(() => {
    return jwtDecoded;
  }, [jwtDecoded]);

  const setJwtToken = useCallback((jwtDecoded) => {
    if (jwtDecoded && jwtDecoded.access_token && jwtDecoded.token_type) {
      setJwt(`${jwtDecoded.token_type} ${jwtDecoded.access_token}`);
      setAutenticated(true);
      setJwtDecoded(jwtDecoded);
    }
  }, [jwt]);

  const logout = useCallback(() => {
    setAutenticated(false);
    setJwt(null);
    setName(null);
  });

  return {
    setJwtToken,
    getJwt,
    isLogged,
    getJwtDecoded,
    logout
  };
};

export const useShareableState = () => useBetween(shareableState);