import React from "react";
import { Button, FormGroup } from 'reactstrap';

const googleOauthUrl = process.env.REACT_APP_GOOGLE_OAUTH_URL;

const style = {
  height: "100vh",
  backgroundColor: "#ddd",
  padding: "20px 10px 10px 10px"
};

const Card = ({ card, bgcolor = "" }) => {
  const cardStyle = { ...style };
  if (bgcolor) {
    cardStyle.backgroundColor = bgcolor;
  }
  const id = `card${card}`;
  return (
    <div id={id} style={cardStyle}>
      <h1>
        <br />
        Card #{card}
        {card === '4' && <div style={{
          marginLeft: '50pc',
          padding: '15px 20px 0 20px'
      }}>
        <FormGroup verticalAlign="center" className="col-md-3 mb-3">
            <Button color="primary" block type="button" onClick={() => window.location.href=`${googleOauthUrl}`}>
              <i className="fa fa-fw fa-google" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} /> Google login
            </Button>
        </FormGroup>
            </div>}
      </h1>
    </div>
  );
};

export default Card;