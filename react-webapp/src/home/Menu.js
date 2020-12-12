import React from "react";
import ScrollTo from "react-scroll-into-view";
import styled from "styled-components";

const Nav = styled.nav`
  position: fixed;
  top: 0;
  text-align: center;
`;
const JumpText = styled.li`
  margin-bottom: 10px;
  display: block;
  font-size: 16px;
  font-weight: 700;
`;

const Button = styled.button`
  padding: 15px 30px;
  font-size: 20px;
  font-weight: 900;
  color: #f50057;
  border: 2px solid #f50057;
  cursor: pointer;
  width: max-content;
  transition: 0.25s ease;
  outline: none;
  :hover {
    border-radius: 25px;
  }
`;

const Menu = ({ cards }) => (
  <Nav>
    <ul>
      <JumpText>spendingbetter.com:</JumpText>
      {Array.from({ length: 4 }, (_, i) => i + 1).map(card => (
        <li key={card}>
          <ScrollTo selector={`#card${card}`}>
            <Button>{card === 1 ? 'Control your spending' : card === 2 ? 'Integrate with your Bank' : card === 3 ? 'Receive Notifications for over budget' : 'Login with Social Media'}</Button>
          </ScrollTo>
        </li>
      ))}
    </ul>
  </Nav>
);

export default Menu;
