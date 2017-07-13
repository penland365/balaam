"use strict";

const React       = require('react');

class AppPane extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
	return (
		<h1>Hello, from React Components!</h1>
	);
  }
}

module.exports = AppPane;
