import React, { useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Header.css';
import { GenderContext } from './GenderContext';

interface HeaderProps {
    onSearch?: (searchTerm: string, genderFilter: string) => void;
}

const Header: React.FC<HeaderProps> = ({ onSearch }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const { genderFilter, setGenderFilter } = useContext(GenderContext);
    const navigate = useNavigate();

    const handleSearch = () => {
        const trimmedSearchTerm = searchTerm.trim();
        if (trimmedSearchTerm.length < 2) {
            alert('검색어는 두 글자 이상이 필요합니다.');
            return;
        }

        if (onSearch) {
            onSearch(searchTerm, genderFilter);
        } else {
            navigate(`/search?query=${encodeURIComponent(searchTerm)}&gender=${genderFilter}`);
        }
    };

    const handleKeyPress = (event: React.KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            handleSearch();
        }
    };

    const handleClear = () => {
        setSearchTerm('');
    };

    const handleGenderFilter = (filter: string) => {
        console.log('Setting gender filter to:', filter);
        setGenderFilter(filter);
    };

    const handleGoHome = () => {
        navigate('/');
    };

    return (
        <header className="header">
            <img
                src="/images/V-Song.png"
                alt="V-Song Logo"
                className="logo"
                onClick={handleGoHome}
                style={{ cursor: 'pointer', height: '40px' }}
            />

            <div className="search-bar">
                <input
                    type="text"
                    placeholder="검색"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={handleKeyPress}
                />
                {searchTerm && (
                    <button type="button" className="clear-btn" onClick={handleClear}>
                        &#x2715;
                    </button>
                )}
                <button type="submit" className="search-btn" onClick={handleSearch}>
                    <img src="/images/SearchBar.png" alt="Search" />
                </button>
            </div>

            <div className="gender-filters">
                <button
                    className={`gender-btn ${genderFilter === 'male' ? 'active' : ''}`}
                    onClick={() => handleGenderFilter('male')}
                >
                    남성
                </button>
                <button
                    className={`gender-btn ${genderFilter === 'female' ? 'active' : ''}`}
                    onClick={() => handleGenderFilter('female')}
                >
                    여성
                </button>
                <button
                    className={`gender-btn ${genderFilter === 'all' ? 'active' : ''}`}
                    onClick={() => handleGenderFilter('all')}
                >
                    전체
                </button>
            </div>
        </header>
    );
};

export default Header;
