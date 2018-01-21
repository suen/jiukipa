import React from 'react'
import ReactDOM from 'react-dom'
import './gallery-grid.css'
import './custom.css'
import {RxHttpRequest} from 'rx-http-request' 

function Image(props) {
	return (
<div className="col-sm-6 col-md-4">
	<a className="lightbox" onClick={props.onClick}>
		 <img src={props.imageThumbsnail} />
	</a>
</div>
	);
}

function ImageLightBox(props) {
	var style = {
		display: props.show || "none",
	};
	console.log(style);
	return (
		<div className="container-fluid lightframe" style={style} onClick={props.onClick}>
			<div className="container">
				<div className="row">
					<div className="col-md-12 col-sm-12 ">
						<p className="centered">
						<img src={props.url} onClick={(evnt) => evnt.stopPropagation()}/>
						</p>
					</div>
				</div>
			</div>
		</div>
	);
}

class Gallery extends React.Component {

	constructor(props){
		super(props);
		this.state = {
			beginDate : "2015-01-01",
			endDate: "2018-01-01",
			frontImage: "",
			images: []
		}
		this.handleInputChange = this.handleInputChange.bind(this);
		this.handleSummit = this.handleSummit.bind(this);
		//setInterval(this.updateTime, 100, this);
	}

	updateTime(that) {
		that.setState({ time : that.getTime()});
	}

	getTime(){
		return new Date().getTime();
	}

	shouldComponentUpdate() {
		return true; 
	}

	componentDidMount() {
		RxHttpRequest.get("http://localhost:8080/images")
			.subscribe(data => {
			const response = JSON.parse(data.body)
			console.log(response);
				this.setState({images : response.images});
			},
			error => console.error(error))
	}

	handleInputChange(event) {
		this.setState({ beginDate: event.target.value});
		console.log("Begin date: " + this.state.beginDate);
		console.log("End date: " + this.state.endDate);
	}

	handleSummit(event) {
		console.log("Begin date: " + this.state.beginDate);
		console.log("End date: " + this.state.endDate);
		alert("hello world");
		event.preventDefault();
	}

	handleClick(image) {
		this.setState({ frontImage : "http://localhost:8080/image/" + image.hash + "/stdsize/FHD"});
	}

	handleLightBoxClick() {
		this.setState({ frontImage : "" });
	}

	render () {
		const images = this.state.images || [];
		const lightbox = this.state.frontImage ? "block" : "none";
		return (
<div>
<ImageLightBox show={lightbox} url={this.state.frontImage} onClick={() => this.handleLightBoxClick() } />
<div className="container gallery-container">
    <h1>My Gallery</h1>
		<h2>Query : </h2>
		<form onSubmit={this.handleSummit}>
			<label>Begin date: <input type="text" value={this.state.beginDate} onChange={this.handleInputChange} /></label>
			<label>End date: <input type="text" value={this.state.endDate} onChange={this.handleInputChange} /></label>
			<input type="submit" value="Search" />
		</form>
    <p className="page-description text-center">Title</p>
    <div className="tz-gallery">
        <div className="row">
						{ images.map((image, index) => (
							<Image key={image.hash} imageFHD={"http://localhost:8080/image/" + image.hash +"/stdsize/FHD"} imageThumbsnail={"http://localhost:8080/image/" + image.hash + "/stdsize/thumbsnail" } onClick={() => this.handleClick(image) }/>
						))}
    		</div>
    </div>
</div>
</div>
		);
	}

}

ReactDOM.render(
  <Gallery />,
  document.getElementById('root')
);
