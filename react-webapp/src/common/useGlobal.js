import React from "react";
import useGlobalHook from "use-global-hook";

const initialState = {
  expanded: false
};

export const shareExpanded = (store, expanded) => {
    store.setState({ expanded: expanded });
};

const useGlobal = useGlobalHook(React, initialState, shareExpanded);

export default useGlobal;